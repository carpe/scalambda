package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}
import java.nio.file.Files

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.FunctionRoleSource
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.resources.{LambdaFunction, LambdaLayerVersion, S3Bucket, S3BucketItem}

object ScalambdaTerraform {

  def writeTerraform(functions: List[ScalambdaFunction], s3BucketName: String, projectSource: File, dependencies: File, terraformOutput: File): Unit = {

    // create archive file resources to use to zip the assembly output in TF
    createArchives(projectSource, dependencies, terraformOutput.getAbsolutePath)

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val (s3Bucket, projectBucketItem, dependenciesBucketItem) = defineS3Resources(s3BucketName)

    // create resource definitions for the lambda functions
    val (lambdas, lambdaDependenciesLayer, lambdaVariables) = defineLambdaResources(functions, s3Bucket, projectBucketItem, dependenciesBucketItem)

    // if there are functions that are configured to be connected to an API Gateway instance
    if (functions.count(_.apiConfig.isDefined) > 0) {
      // create open api for functions
      val openApi = OpenApi.forFunctions(functions)

      // write the swagger to a file
      writeSwagger(terraformOutput.getAbsolutePath, openApi)
    }

    // load those sources into a module
    val scalambdaModule = ScalambdaModule(
      lambdas, lambdaDependenciesLayer,
      s3Buckets = Seq(s3Bucket),
      s3BucketItems = Seq(projectBucketItem, dependenciesBucketItem),
      sources = Seq(),

      variables = lambdaVariables
    )


    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

  def defineLambdaResources( scalambdaFunctions: List[ScalambdaFunction],
                             s3Bucket: S3Bucket,
                             projectBucketItem: S3BucketItem,
                             dependenciesBucketItem: S3BucketItem
                           ): (Seq[LambdaFunction], LambdaLayerVersion, Seq[Variable[_]]) = {
    // create a lambda layer that can be shared by all functions that contains the dependencies of said functions.
    // this will be used to speed up deployments
    val lambdaDependenciesLayer = LambdaLayerVersion(dependenciesBucketItem)

    // create resources for each of the lambda functions and the variables they require
    val (lambdaFunctions, lambdaVariables) = scalambdaFunctions.foldRight(Seq.empty[LambdaFunction], Seq.empty[Variable[_]])((function: ScalambdaFunction, resources) => {
      val (functionResources, variables) = resources

      val functionResource = LambdaFunction(function, s3Bucket, projectBucketItem, lambdaDependenciesLayer)
      val functionVariables = Seq(
        function.iamRole match {
          case fromVariable: FunctionRoleSource.FromVariable.type =>
            Some(Variable[TString](fromVariable.variableName(function), description = Some(s"Arn for the IAM Role to be used by the ${function.approximateFunctionName} Lambda Function."), defaultValue = None))
          case FunctionRoleSource.StaticArn(_) =>
            None
        }
      ).flatten

      (functionResources :+ functionResource, variables ++ functionVariables)
    })

    (lambdaFunctions, lambdaDependenciesLayer, lambdaVariables)
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

  def writeSwagger(rootTerraformPath: String, openApi: OpenApi): Unit = {
    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }
  }
}
