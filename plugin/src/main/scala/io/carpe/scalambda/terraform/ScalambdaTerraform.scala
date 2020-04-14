package io.carpe.scalambda.terraform

import java.io.File
import java.nio.file.Files

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.ProjectFunction
import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.data
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}
import io.carpe.scalambda.terraform.composing.{ApiGatewayComposer, LambdaComposer}

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

    // get filename of for the dependencies jar, since it has a content hash appended to it that we can use to power
    // our caching mechanism
    val dependenciesFileNameWithContentHash = dependencies.getName

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val (s3Bucket, projectBucketItem, dependenciesBucketItem) = defineS3Resources(s3BucketName, dependenciesFileNameWithContentHash)

    val projectFunctions = functions.flatMap(_ match {
      case function: ProjectFunction =>
        Some(function)
      case ScalambdaFunction.ReferencedFunction(_, _, _) =>
        None
    })

    val referencedFunctionAliases = functions.flatMap(_ match {
      case function: ProjectFunction =>
        None
      case referencedFunction: ScalambdaFunction.ReferencedFunction =>
        Some(data.LambdaFunctionAliasData(referencedFunction))
    })

    // create resource definitions for the lambda functions
    val versionAsAlias = StringUtils.toSnakeCase(version)
    val (lambdas, lambdaAliases, lambdaDependenciesLayer, lambdaWarming, lambdaVariables, lambdaOutputs) =
      LambdaComposer.defineLambdaResources(
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
      apiRoute53Alias,
      apiVariables
    ) =
      ApiGatewayComposer.maybeDefineApiResources(
        isXrayEnabled,
        apiName,
        functions,
        lambdaAliases ++ referencedFunctionAliases,
        terraformOutput,
        maybeDomainName
      )

    // load resources into module
    val scalambdaModule = ScalambdaModule(
      lambdas,
      lambdaAliases ++ referencedFunctionAliases,
      lambdaDependenciesLayer,
      lambdaWarmingResources = lambdaWarming,
      s3Buckets = Seq(s3Bucket),
      s3BucketItems = Seq(projectBucketItem, dependenciesBucketItem),
      sources = Seq(),
      apiGateway = apiGateway,
      lambdaPermissions = lambdaPermissions,
      swaggerTemplate = swaggerTemplate,
      apiGatewayDeployment = apiGatewayDeployment,
      apiGatewayStage = apiGatewayStage,
      domainResources = Seq(apiDomainName, apiPathMapping, apiRoute53Alias).flatten,
      variables = lambdaVariables ++ apiVariables,
      outputs = lambdaOutputs
    )

    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

  def defineS3Resources(bucketName: String, dependenciesPathWithContentHash: String): (S3Bucket, S3BucketItem, S3BucketItem) = {
    // create a new bucket
    val newBucket = S3Bucket(bucketName)

    // create bucket items (to be placed into that bucket) that are pointed to the sources of each lambda function
    val sourceBucketItem =
      S3BucketItem(newBucket, name = "sources", key = "sources.jar", source = "sources.jar", etag = TLiteral("""filemd5("${path.module}/sources.jar")"""))
    val depsBucketItem = S3BucketItem(
      newBucket,
      name = "dependencies",
      key = "dependencies.zip",
      source = "dependencies.zip",
      etag = TLiteral("""filemd5("${path.module}/dependencies.zip")""")
    )

    (newBucket, sourceBucketItem, depsBucketItem)
  }
}
