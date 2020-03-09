package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}
import java.nio.file.Files

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.FunctionRoleSource
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.data.ArchiveFile
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.resources.{LambdaFunction, S3Bucket, S3BucketItem}

object ScalambdaTerraform {

  def writeTerraform(functions: List[ScalambdaFunction], s3BucketName: String, projectSource: File, dependencies: File, terraformOutput: File): Unit = {

    // create archive file resources to use to zip the assembly output in TF
    val (projectArchive, depsArchive) = createArchives(projectSource, dependencies, terraformOutput.getAbsolutePath)

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val (s3Bucket, projectBucketItem, dependenciesBucketItem) = defineS3Resources(s3BucketName, projectArchive, depsArchive)

    // create resource definitions for the lambda functions
    val (lambdas, lambdaVariables) = defineLambdaResources(functions, s3Bucket, projectBucketItem)

    // if there are functions that are configured to be connected to an API Gateway instance
    if (functions.count(_.apiConfig.isDefined) > 0) {
      // create open api for functions
      val openApi = OpenApi.forFunctions(functions)

      // write the swagger to a file
      writeSwagger(terraformOutput.getAbsolutePath, openApi)
    }

    // load those sources into a module
    val scalambdaModule = ScalambdaModule(
      lambdas, s3Buckets = Seq(s3Bucket),
      s3BucketItems = Seq(projectBucketItem, dependenciesBucketItem),
      sources = Seq(projectArchive, depsArchive),

      variables = lambdaVariables
    )


    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

  def defineLambdaResources(scalambdaFunctions: List[ScalambdaFunction], s3Bucket: S3Bucket, projectBucketItem: S3BucketItem): (Seq[LambdaFunction], Seq[Variable[_]]) = {
    scalambdaFunctions.foldRight(Seq.empty[LambdaFunction], Seq.empty[Variable[_]])((function: ScalambdaFunction, resources) => {
      val (functionResources, variables) = resources

      val functionResource = LambdaFunction(function, s3Bucket, projectBucketItem)
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
  }

  def createArchives(source: File, dependencies: File, output: String): (ArchiveFile, ArchiveFile) = {

    def copyFile(from: File, to: String) = {
      import java.io.{File, FileInputStream, FileOutputStream}
      val dest = new File(to)
      Files.createDirectories(new File(output).toPath)
      new FileOutputStream(dest) getChannel() transferFrom(
        new FileInputStream(from).getChannel, 0, Long.MaxValue )
    }

    val sourcesOutputPath = output + s"/source.jar"
    copyFile(source, sourcesOutputPath)
    val sourcesArchive = ArchiveFile("sources", sourcesOutputPath, "sources.zip")

    val dependenciesOutputPath = output + s"/dependencies.jar"
    copyFile(dependencies, dependenciesOutputPath)
    val dependenciesArchive = ArchiveFile("dependencies", sourcesOutputPath, "dependencies.zip")

    (sourcesArchive, dependenciesArchive)
  }

  def defineS3Resources(bucketName: String, sourceArchive: ArchiveFile, depsArchive: ArchiveFile): (S3Bucket, S3BucketItem, S3BucketItem) = {
    // create a new bucket
    val newBucket = S3Bucket(bucketName)

    // create bucket items (to be placed into that bucket) that are pointed to the sources of each lambda function
    val sourceBucketItem = S3BucketItem(newBucket, name = "sources", key = sourceArchive.name, source = sourceArchive)
    val depsBucketItem = S3BucketItem(newBucket, name = "dependencies", key = depsArchive.name, source = depsArchive)

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
