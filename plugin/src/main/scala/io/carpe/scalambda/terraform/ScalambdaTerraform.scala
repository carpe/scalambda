package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}
import java.nio.file.Files

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.ProjectFunction
import io.carpe.scalambda.conf.function.{EnvironmentVariable, FunctionRoleSource}
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.data.TemplateFile
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.resources._

object ScalambdaTerraform {

  def writeTerraform(functions: List[ScalambdaFunction], s3BucketName: String, projectSource: File, dependencies: File, apiName: String, terraformOutput: File): Unit = {

    // create archive file resources to use to zip the assembly output in TF
    createArchives(projectSource, dependencies, terraformOutput.getAbsolutePath)

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val (s3Bucket, projectBucketItem, dependenciesBucketItem) = defineS3Resources(s3BucketName)

    val projectFunctions = functions.flatMap(_ match {
      case function: ProjectFunction =>
        Some(function)
      case ScalambdaFunction.ReferencedFunction(functionName, _, functionArn, apiGatewayConf) =>
        None
    })

    // create resource definitions for the lambda functions
    val (lambdas, lambdaDependenciesLayer, lambdaVariables) = defineLambdaResources(projectFunctions, s3Bucket, projectBucketItem, dependenciesBucketItem)

    // create resource definitions for an api gateway instance, if lambdas are configured for HTTP
    val (apiGateway,  swaggerTemplate, lambdaPermissions, apiGatewayDeployment) = maybeDefineApiResources(apiName, functions, terraformOutput)

    // load resources into module
    val scalambdaModule = ScalambdaModule(
      lambdas, lambdaDependenciesLayer,
      s3Buckets = Seq(s3Bucket),
      s3BucketItems = Seq(projectBucketItem, dependenciesBucketItem),
      sources = Seq(),
      apiGateway = apiGateway,
      lambdaPermissions = lambdaPermissions,
      swaggerTemplate = swaggerTemplate,
      apiGatewayDeployment = apiGatewayDeployment,

      variables = lambdaVariables
    )

    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

  def defineLambdaResources( scalambdaFunctions: List[ProjectFunction],
                             s3Bucket: S3Bucket,
                             projectBucketItem: S3BucketItem,
                             dependenciesBucketItem: S3BucketItem
                           ): (Seq[LambdaFunction], LambdaLayerVersion, Seq[Variable[_]]) = {
    // create a lambda layer that can be shared by all functions that contains the dependencies of said functions.
    // this will be used to speed up deployments
    val lambdaDependenciesLayer = LambdaLayerVersion(dependenciesBucketItem)

    // create resources for each of the lambda functions and the variables they require
    val (lambdaFunctions, lambdaVariables) = scalambdaFunctions.foldRight(Seq.empty[LambdaFunction], Seq.empty[Variable[_]])((function: ProjectFunction, resources) => {
      val (functionResources, variables) = resources

      val functionResource = LambdaFunction(function, s3Bucket, projectBucketItem, lambdaDependenciesLayer)
      val functionVariables = Seq(
        function match {
          case f: ScalambdaFunction.Function =>
            f.iamRole match {
              case fromVariable: FunctionRoleSource.FromVariable.type =>
                Some(Variable[TString](fromVariable.variableName(f), description = Some(s"Arn for the IAM Role to be used by the ${f.approximateFunctionName} Lambda Function."), defaultValue = None))
              case FunctionRoleSource.StaticArn(_) =>
                None
            }
          case af: ScalambdaFunction.ApiFunction =>
            af.iamRole match {
              case fromVariable: FunctionRoleSource.FromVariable.type =>
                Some(Variable[TString](fromVariable.variableName(af), description = Some(s"Arn for the IAM Role to be used by the ${af.approximateFunctionName} Lambda Function."), defaultValue = None))
              case FunctionRoleSource.StaticArn(_) =>
                None
            }
        }
      ).flatten ++ function.environmentVariables.flatMap(_ match {
        case EnvironmentVariable.Static(key, value) =>
          None
        case EnvironmentVariable.Variable(key, variableName) =>
          Some(variableName)
      }).map(variable => Variable[TString](variable, description = Some("Injected as ENV variable into lambda functions"), defaultValue = None))

      (functionResources :+ functionResource, variables ++ functionVariables)
    })

    (lambdaFunctions, lambdaDependenciesLayer, lambdaVariables.distinct)
  }

  def createArchives(source: File, dependencies: File, output: String): Unit = {

    def copyFile(from: File, to: String) = {
      import java.io.{File, FileInputStream, FileOutputStream}
      val dest = new File(to)
      Files.createDirectories(dest.getParentFile.toPath)
      new FileOutputStream(dest) getChannel() transferFrom(
        new FileInputStream(from).getChannel, 0, Long.MaxValue )
    }

    val sourcesOutputPath = output + "/sources.jar"
    copyFile(source, sourcesOutputPath)

    // we need to store the dependencies in a specific folder structure in order to have them loaded into the lambda layer
    import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    // create zip file of dependencies jar because terraform's archive_file is acting strange in recent versions
    // TODO: review this and see if we can switch back to using a terraform archive
    val zip = new ZipOutputStream(new FileOutputStream(output + "/dependencies.zip"))
    zip.putNextEntry(new ZipEntry("java/lib/dependencies.jar"))
    val in = new BufferedInputStream(new FileInputStream(dependencies))
    var b = in.read()
    while (b > -1) {
      zip.write(b)
      b = in.read()
    }
    in.close()
    zip.closeEntry()
    zip.close()
  }

  def defineS3Resources(bucketName: String): (S3Bucket, S3BucketItem, S3BucketItem) = {
    // create a new bucket
    val newBucket = S3Bucket(bucketName)

    // create bucket items (to be placed into that bucket) that are pointed to the sources of each lambda function
    val sourceBucketItem = S3BucketItem(newBucket, name = "sources", key = "sources.jar", source = "sources.jar", etag = "sources.jar")
    val depsBucketItem = S3BucketItem(newBucket, name = "dependencies", key = "dependencies.zip", source = "dependencies.zip", etag = "dependencies.zip")

    (newBucket, sourceBucketItem, depsBucketItem)
  }

  def maybeDefineApiResources(apiName: String, functions: Seq[ScalambdaFunction], terraformOutput: File): (Option[ApiGateway], Option[TemplateFile], Seq[LambdaPermission], Option[ApiGatewayDeployment]) = {

    // if there are no functions that are configured to be exposed via api
    if (functions.count(_.apiGatewayConfig.isDefined) == 0) {
      // do an early return
      return (None, None, Seq.empty, None)
    }

    def writeSwagger(apiName: String, functions: Seq[ScalambdaFunction], rootTerraformPath: String): TemplateFile = {
      val openApi = OpenApi.forFunctions(functions)

      // convert the api to yaml
      val openApiDefinition = OpenApi.apiToYaml(openApi)

      // write that yaml to the path
      val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
      new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }

      TemplateFile("swagger.yaml", apiName, functions)
    }
    val swaggerTemplate = writeSwagger(apiName = apiName, rootTerraformPath = terraformOutput.getAbsolutePath, functions = functions)

    // define api gateway
    val api = ApiGateway(TString(apiName), None, swaggerTemplate)

    // define permissions for api gateway to invoke lambdas
    val permissions = functions.map(function => {
      LambdaPermission(function, api)
    })

    // create deployment
    val apiGatewayDeployment = ApiGatewayDeployment(api, permissions)

    (Some(api), Some(swaggerTemplate), permissions, Some(apiGatewayDeployment))
  }
}
