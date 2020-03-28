package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}
import java.nio.file.Files

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.ProjectFunction
import io.carpe.scalambda.conf.function.{EnvironmentVariable, FunctionRoleSource}
import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.data.TemplateFile
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.props.TValue.{TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.resources.{apigateway, lambda, _}
import io.carpe.scalambda.terraform.ast.resources.apigateway.{ApiGateway, ApiGatewayBasePathMapping, ApiGatewayDeployment, ApiGatewayDomainName, ApiGatewayStage}
import io.carpe.scalambda.terraform.ast.resources.lambda.{LambdaFunction, LambdaFunctionAlias, LambdaLayerVersion, LambdaPermission, ProvisionedConcurrency}

object ScalambdaTerraform {

  def writeTerraform(
    projectName: String,
    functions: List[ScalambdaFunction],
    version: String,
    s3BucketName: String,
    projectSource: File,
    dependencies: File,
    isXrayEnabled: Boolean,
    apiName: String,
    terraformOutput: File,
    maybeDomainName: Option[String]
  ): Unit = {

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
    val versionAsAlias = StringUtils.toSnakeCase(version)
    val (lambdas, lambdaAliases, lambdaDependenciesLayer, lambdaConcurrencies, lambdaVariables, lambdaOutputs) =
      defineLambdaResources(
        isXrayEnabled,
        projectName,
        projectFunctions,
        versionAsAlias,
        s3Bucket,
        projectBucketItem,
        dependenciesBucketItem
      )

    // create resource definitions for an api gateway instance, if lambdas are configured for HTTP
    val (
      apiGateway,
      swaggerTemplate,
      lambdaPermissions,
      apiGatewayDeployment,
      apiGatewayStage,
      apiDomainName,
      apiPathMapping,
      apiVariables
    ) =
      maybeDefineApiResources(isXrayEnabled, apiName, lambdaAliases, terraformOutput, maybeDomainName)

    // load resources into module
    val scalambdaModule = ScalambdaModule(
      lambdas,
      lambdaAliases,
      lambdaDependenciesLayer,
      lambdaProvisionedCurrencies = lambdaConcurrencies,
      s3Buckets = Seq(s3Bucket),
      s3BucketItems = Seq(projectBucketItem, dependenciesBucketItem),
      sources = Seq(),
      apiGateway = apiGateway,
      lambdaPermissions = lambdaPermissions,
      swaggerTemplate = swaggerTemplate,
      apiGatewayDeployment = apiGatewayDeployment,
      apiGatewayStage = apiGatewayStage,
      apiGatewayDomainName = apiDomainName,
      apiGatewayBasePathMapping = apiPathMapping,
      variables = lambdaVariables ++ apiVariables,
      outputs = lambdaOutputs
    )

    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

  def defineLambdaResources(
    isXrayEnabled: Boolean,
    projectName: String,
    scalambdaFunctions: List[ProjectFunction],
    version: String,
    s3Bucket: S3Bucket,
    projectBucketItem: S3BucketItem,
    dependenciesBucketItem: S3BucketItem
  ): (
    Seq[LambdaFunction],
    Seq[LambdaFunctionAlias],
    LambdaLayerVersion,
    Seq[ProvisionedConcurrency],
    Seq[Variable[_]],
    Seq[Output[_]]
  ) = {
    // create a lambda layer that can be shared by all functions that contains the dependencies of said functions.
    // this will be used to speed up deployments
    val layerName = s"${StringUtils.toSnakeCase(projectName)}_assembled_dependencies"
    val lambdaDependenciesLayer = lambda.LambdaLayerVersion(layerName, dependenciesBucketItem)

    // create resources for each of the lambda functions and the variables they require
    val (lambdaFunctions, lambdaAliases, lambdaConcurrencies, lambdaVariables, outputs) =
      scalambdaFunctions.foldRight(
        Seq.empty[LambdaFunction],
        Seq.empty[LambdaFunctionAlias],
        Seq.empty[ProvisionedConcurrency],
        Seq.empty[Variable[_]],
        Seq.empty[Output[_]]
      )(
        (function: ProjectFunction, resources) => {
          val (
            functionResources,
            functionAliases,
            functionConcurrencies: Seq[ProvisionedConcurrency],
            variables,
            outputs
          ) = resources

          /**
           * Define Terraform resources for function
           */
          val functionResource =
            lambda.LambdaFunction(function, version, s3Bucket, projectBucketItem, lambdaDependenciesLayer, isXrayEnabled)

          val functionAlias = LambdaFunctionAlias(functionResource, version)

          val functionConcurrency: Seq[ProvisionedConcurrency] = {
            if (function.provisionedConcurrency > 0) {
              Seq(ProvisionedConcurrency(functionAlias, function.provisionedConcurrency))
            } else {
              Nil
            }
          }

          /**
           * Define Terraform variables for function
           */
          val functionRoleVariables = Seq(
            function.iamRole match {
              case fromVariable: FunctionRoleSource.FromVariable.type =>
                Some(
                  Variable[TString](
                    fromVariable.variableName(function),
                    description = Some(
                      s"Arn for the IAM Role to be used by the ${function.approximateFunctionName} Lambda Function."
                    ),
                    defaultValue = None
                  )
                )
              case FunctionRoleSource.StaticArn(_) =>
                None
            }
          ).flatten

          val functionEnvVariables = function
            .environmentVariables
            .flatMap(_ match {
              case EnvironmentVariable.Static(key, value) =>
                None
              case EnvironmentVariable.Variable(key, variableName) =>
                Some(variableName)
            })
            .map(
              variable =>
                Variable[TString](
                  variable,
                  description = Some("Injected as ENV variable into lambda functions"),
                  defaultValue = None
                )
            )

          val functionVariables = functionRoleVariables ++ functionEnvVariables

          /**
           * Define Terraform outputs for function
           */
          val functionOutputs = Seq(
            Output(
              name = s"${function.terraformLambdaResourceName}_arn",
              description = Some(s"Arn for the ${function.approximateFunctionName}"),
              isSensitive = false,
              value = TResourceRef("aws_lambda_function", function.terraformLambdaResourceName, "arn")
            )
          )

          (
            functionResources :+ functionResource,
            functionAliases :+ functionAlias,
            (functionConcurrencies ++ functionConcurrency),
            variables ++ functionVariables,
            outputs ++ functionOutputs
          )
        }
      )

    (lambdaFunctions, lambdaAliases, lambdaDependenciesLayer, lambdaConcurrencies, lambdaVariables.distinct, outputs)
  }

  def createArchives(source: File, dependencies: File, output: String): Unit = {

    def copyFile(from: File, to: String) = {
      import java.io.{File, FileInputStream, FileOutputStream}
      val dest = new File(to)
      Files.createDirectories(dest.getParentFile.toPath)
      new FileOutputStream(dest).getChannel.transferFrom(new FileInputStream(from).getChannel, 0, Long.MaxValue)
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
    val sourceBucketItem =
      S3BucketItem(newBucket, name = "sources", key = "sources.jar", source = "sources.jar", etag = "sources.jar")
    val depsBucketItem = S3BucketItem(
      newBucket,
      name = "dependencies",
      key = "dependencies.zip",
      source = "dependencies.zip",
      etag = "dependencies.zip"
    )

    (newBucket, sourceBucketItem, depsBucketItem)
  }

  def maybeDefineApiResources(
    isXrayEnabled: Boolean,
    apiName: String,
    functionAliases: Seq[LambdaFunctionAlias],
    terraformOutput: File,
    maybeDomainName: Option[String]
  ): (
    Option[ApiGateway],
    Option[TemplateFile],
    Seq[LambdaPermission],
    Option[ApiGatewayDeployment],
    Option[ApiGatewayStage],
    Option[ApiGatewayDomainName],
    Option[ApiGatewayBasePathMapping],
    Seq[Variable[_]]
  ) = {

    // if there are no functions that are configured to be exposed via api
    if (functionAliases.count(_.function.scalambdaFunction.apiGatewayConfig.isDefined) == 0) {
      // do an early return
      return (None, None, Seq.empty, None, None, None, None, Seq.empty)
    }

    def writeSwagger(apiName: String, aliases: Seq[LambdaFunctionAlias], rootTerraformPath: String): TemplateFile = {
      val openApi = OpenApi.forFunctions(aliases.map(_.function.scalambdaFunction))

      // convert the api to yaml
      val openApiDefinition = OpenApi.apiToYaml(openApi)

      // write that yaml to the path
      val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
      new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }

      TemplateFile("swagger.yaml", apiName, aliases)
    }
    val swaggerTemplate =
      writeSwagger(apiName = apiName, rootTerraformPath = terraformOutput.getAbsolutePath, aliases = functionAliases)

    // define api gateway
    val api = ApiGateway(TString(apiName), None, swaggerTemplate)

    // define permissions for api gateway to invoke lambdas
    val permissions = functionAliases.map(alias => {
      LambdaPermission(alias, api)
    })

    // create deployment
    val apiGatewayDeployment = ApiGatewayDeployment(api, permissions)

    // create stage
    val apiGatewayStage = ApiGatewayStage(api, apiGatewayDeployment, isXrayEnabled)

    // create resources for domain mapping (if domain name was supplied)
    val maybeApiGatewayDomainName = maybeDomainName.map(domainName => ApiGatewayDomainName(domainName))
    val maybeBasePathMapping = maybeApiGatewayDomainName.map(domainName => {
      apigateway.ApiGatewayBasePathMapping(api, apiGatewayDeployment, domainName)
    })
    val certificateVariable = maybeDomainName
      .map(_ => Variable("certificate_arn", Some("Arn of AWS Certificate Manager certificate."), None))
      .toSeq

    (
      Some(api),
      Some(swaggerTemplate),
      permissions,
      Some(apiGatewayDeployment),
      Some(apiGatewayStage),
      maybeApiGatewayDomainName,
      maybeBasePathMapping,
      certificateVariable
    )
  }
}
