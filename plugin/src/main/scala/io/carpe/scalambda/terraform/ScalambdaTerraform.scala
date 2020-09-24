package io.carpe.scalambda.terraform

import java.io.File

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.DefinedFunction
import io.carpe.scalambda.conf.api.{ApiDomain, ApiGatewayEndpoint}
import io.carpe.scalambda.conf.function.FunctionSources
import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.module.ScalambdaModule
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.data
import io.carpe.scalambda.terraform.composing.S3Composer.S3Composable
import io.carpe.scalambda.terraform.composing.{ApiGatewayComposer, LambdaComposer, S3Composer}

object ScalambdaTerraform {

  def writeTerraform(projectName: String,
                     functions: List[ScalambdaFunction],
                     functionSources: FunctionSources,
                     endpointMappings: List[(ApiGatewayEndpoint, ScalambdaFunction)],
                     version: String,
                     s3BucketName: String,
                     billingTags: Seq[BillingTag],
                     isXrayEnabled: Boolean,
                     apiName: String,
                     terraformOutput: File,
                     domainNameMapping: ApiDomain
                    ): Unit = {

    // create resource definitions for the s3 resources that will be used to store the function's source code
    val s3Composable = S3Composable(
      functionSources = functionSources,
      s3BucketName = s3BucketName,
      billingTags = billingTags
    )
    val s3Resources = S3Composer.defineS3Resources(s3Composable)

    val projectFunctions = functions.flatMap(_ match {
      case function: DefinedFunction =>
        Some(function)
      case ScalambdaFunction.ReferencedFunction(_, _, _) =>
        None
    })

    val referencedFunctionAliases = functions.flatMap(_ match {
      case function: DefinedFunction =>
        None
      case referencedFunction: ScalambdaFunction.ReferencedFunction =>
        Some(data.LambdaFunctionAliasData(referencedFunction))
    })

    // create resource definitions for the lambda functions
    val versionAsAlias = StringUtils.toSnakeCase(version)
    val lambdaResources =
      LambdaComposer.defineLambdaResources(
        isXrayEnabled,
        projectName,
        projectFunctions,
        versionAsAlias,
        s3Resources,
        billingTags
      )

    // create resource definitions for an api gateway instance, if lambdas are configured for HTTP
    val (
      apiGateway,
      swaggerTemplate,
      apiLambdaAliases,
      lambdaPermissions,
      apiGatewayDeployment,
      apiGatewayStage,
      apiDomainName,
      apiPathMapping,
      apiRoute53Alias,
      apiVariables,
      apiOutputs
      ) =
      ApiGatewayComposer.maybeDefineApiResources(
        isXrayEnabled,
        apiName,
        endpointMappings,
        lambdaResources.functionAliases ++ referencedFunctionAliases,
        terraformOutput,
        domainNameMapping
      )

    // load resources into module
    val scalambdaModule = ScalambdaModule(
      lambdaResources = lambdaResources.lambdaResources ++ lambdaResources.functionAliases ++ apiLambdaAliases ++ referencedFunctionAliases,
      lambdaWarmingResources = lambdaResources.warmerResources,
      s3Buckets = Seq(s3Resources.bucket),
      s3BucketItems = s3Resources.bucketItems,
      sources = Seq(),
      apiGatewayResources = Seq(apiGateway, swaggerTemplate, apiGatewayDeployment, apiGatewayStage, apiDomainName, apiPathMapping, apiRoute53Alias).flatten ++ lambdaPermissions,
      variables = lambdaResources.variables ++ apiVariables :+ s3Resources.s3BillingTagsVariable,
      outputs = lambdaResources.outputs ++ apiOutputs
    )

    // write the module to a series of files
    ScalambdaModule.write(scalambdaModule, terraformOutput.getAbsolutePath)
  }

}
