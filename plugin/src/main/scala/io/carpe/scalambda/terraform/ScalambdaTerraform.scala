package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.FunctionSource
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.resources.{LambdaFunction, S3Bucket, S3BucketItem}

object ScalambdaTerraform {

  def writeTerraform(functions: List[ScalambdaFunction], assemblyOutput: File, rootTerraformPath: String): Unit = {

    // create resource definitions for the lambda functions
    val lambdas = defineLambdaResources(functions)

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val (s3Buckets, s3BucketItems) = defineS3Resources(functions, assemblyOutput)

    // if there are functions that are configured to be connected to an API Gateway instance
    if (functions.count(_.apiConfig.isDefined) > 0) {
      // create open api for functions
      val openApi = OpenApi.forFunctions(functions)

      // write the swagger to a file
      writeSwagger(rootTerraformPath, openApi)
    }

    // load those sources into a module
    val scalambdaModule = ScalambdaModule(lambdas, s3Buckets = s3Buckets, s3BucketItems = s3BucketItems)

    ScalambdaModule.write(scalambdaModule, rootTerraformPath)
  }

  def defineLambdaResources(scalambdaFunctions: List[ScalambdaFunction]): Seq[LambdaFunction] = {
    scalambdaFunctions.map(function => {
      LambdaFunction(function)
    })
  }

  def defineS3Resources(scalambdaFunctions: List[ScalambdaFunction], assemblyOutput: File): (Seq[S3Bucket], Seq[S3BucketItem]) = {
    val sourcePath = assemblyOutput.getPath

    val initialResources = (Seq.empty[S3Bucket], Seq.empty[S3BucketItem])

    scalambdaFunctions.groupBy(_.s3BucketName).foldRight(initialResources)((bucketNameAndLambdas, resources) => {
      val (bucketName, scalambdas) = bucketNameAndLambdas

      // create a new bucket for the set of lambda functions that use it to store their sources
      val newBucket = S3Bucket(bucketName)

      // create bucket items (to be placed into that bucket) that are pointed to the sources of each lambda function
      val newBucketItems = scalambdas.foldRight(Seq.empty[S3BucketItem])((function, bucketItems) => {
        bucketItems :+ S3BucketItem(newBucket, key = function.terraformS3BucketItemResourceName, source = sourcePath)
      })

      // append the new bucket and bucket items
      val (buckets, bucketItems) = resources
      (buckets :+ newBucket, bucketItems ++ newBucketItems)
    })
  }

  def writeSwagger(rootTerraformPath: String, openApi: OpenApi): Unit = {
    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }
  }
}
