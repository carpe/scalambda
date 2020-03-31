package io.carpe.scalambda.terraform.ast.module

import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.TerraformFile
import io.carpe.scalambda.terraform.ast.data.LambdaFunctionAlias.DataLambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.data.{ArchiveFile, TemplateFile}
import io.carpe.scalambda.terraform.ast.resources.apigateway.{ApiGateway, ApiGatewayBasePathMapping, ApiGatewayDeployment, ApiGatewayDomainName, ApiGatewayStage}
import io.carpe.scalambda.terraform.ast.resources._
import io.carpe.scalambda.terraform.ast.resources.lambda.{LambdaFunction, LambdaFunctionAlias, LambdaLayerVersion, LambdaPermission, ProvisionedConcurrency}

case class ScalambdaModule( // lambda resources
                            lambdas: Seq[LambdaFunction],
                            lambdaAliases: Seq[LambdaFunctionAlias],
                            referencedFunctionAliases: Seq[DataLambdaFunctionAlias],
                            lambdaDependencyLayer: LambdaLayerVersion,
                            lambdaProvisionedCurrencies: Seq[ProvisionedConcurrency],
                            s3Buckets: Seq[S3Bucket],
                            s3BucketItems: Seq[S3BucketItem],
                            sources: Seq[ArchiveFile],

                            // api gateway resources
                            apiGateway: Option[ApiGateway],
                            lambdaPermissions: Seq[LambdaPermission],
                            swaggerTemplate: Option[TemplateFile],
                            apiGatewayDeployment: Option[ApiGatewayDeployment],
                            apiGatewayStage: Option[ApiGatewayStage],
                            apiGatewayDomainName: Option[ApiGatewayDomainName],
                            apiGatewayBasePathMapping: Option[ApiGatewayBasePathMapping],

                            // other
                            variables: Seq[Variable[_]],
                            outputs: Seq[Output[_]]
                          )

object ScalambdaModule {

  def write(scalambdaModule: ScalambdaModule, rootPath: String): Unit = {
    val lambdasFile = TerraformFile((scalambdaModule.lambdaDependencyLayer +: scalambdaModule.lambdas) ++ scalambdaModule.lambdaAliases ++ scalambdaModule.referencedFunctionAliases ++ scalambdaModule.lambdaProvisionedCurrencies, "lambdas.tf")
    val s3File = TerraformFile(scalambdaModule.s3Buckets ++ scalambdaModule.sources ++ scalambdaModule.s3BucketItems, "s3.tf")
    val variablesAndOutputsFile = TerraformFile(scalambdaModule.variables ++ scalambdaModule.outputs, "io.tf")
    val coreLambdaFiles = Seq(lambdasFile, s3File, variablesAndOutputsFile)

    val apiFiles = scalambdaModule match {
      case ScalambdaModule(_, _, _, _, _, _, _, _, Some(apiGateway), _, Some(swaggerTemplate), Some(apiGatewayDeployment), Some(apiGatewayStage), maybeDomainName, maybeBasePathMapping, _, _) =>
        val domainResources = Seq(maybeDomainName, maybeBasePathMapping).flatten
        val apiResources = apiGateway +: swaggerTemplate +: apiGatewayDeployment +: apiGatewayStage +: scalambdaModule.lambdaPermissions

        Seq(
          TerraformFile(domainResources ++ apiResources, "api.tf")
        )
      case _ =>
        Seq.empty
    }

    TerraformFile.writeFiles(coreLambdaFiles ++ apiFiles, rootPath)

    println(s"${coreLambdaFiles.map(_.definitions.size).sum} Terraform definitions were generated. They have been written to: ${rootPath}")
  }
}
