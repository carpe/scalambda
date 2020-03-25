package io.carpe.scalambda.terraform.ast.module

import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.TerraformFile
import io.carpe.scalambda.terraform.ast.data.{ArchiveFile, TemplateFile}
import io.carpe.scalambda.terraform.ast.resources.{ApiGateway, ApiGatewayBasePathMapping, ApiGatewayDeployment, ApiGatewayDomainName, LambdaFunction, LambdaFunctionAlias, LambdaLayerVersion, LambdaPermission, S3Bucket, S3BucketItem}

case class ScalambdaModule( // lambda resources
                            lambdas: Seq[LambdaFunction],
                            lambdaAliases: Seq[LambdaFunctionAlias],
                            lambdaDependencyLayer: LambdaLayerVersion,
                            s3Buckets: Seq[S3Bucket],
                            s3BucketItems: Seq[S3BucketItem],
                            sources: Seq[ArchiveFile],

                            // api gateway resources
                            apiGateway: Option[ApiGateway],
                            lambdaPermissions: Seq[LambdaPermission],
                            swaggerTemplate: Option[TemplateFile],
                            apiGatewayDeployment: Option[ApiGatewayDeployment],
                            apiGatewayDomainName: Option[ApiGatewayDomainName],
                            apiGatewayBasePathMapping: Option[ApiGatewayBasePathMapping],

                            // other
                            variables: Seq[Variable[_]],
                            outputs: Seq[Output[_]]
                          )

object ScalambdaModule {

  def write(scalambdaModule: ScalambdaModule, rootPath: String): Unit = {
    val lambdasFile = TerraformFile((scalambdaModule.lambdaDependencyLayer +: scalambdaModule.lambdas) ++ scalambdaModule.lambdaAliases, "lambdas.tf")
    val s3File = TerraformFile(scalambdaModule.s3Buckets ++ scalambdaModule.sources ++ scalambdaModule.s3BucketItems, "s3.tf")
    val variablesAndOutputsFile = TerraformFile(scalambdaModule.variables ++ scalambdaModule.outputs, "io.tf")
    val coreLambdaFiles = Seq(lambdasFile, s3File, variablesAndOutputsFile)

    val apiFiles = scalambdaModule match {
      case ScalambdaModule(_, _, _, _, _, _, Some(apiGateway), _, Some(swaggerTemplate), Some(apiGatewayDeployment), maybeDomainName, maybeBasePathMapping, _, _) =>
        val domainResources = Seq(maybeDomainName, maybeBasePathMapping).flatten
        val apiResources = apiGateway +: swaggerTemplate +: apiGatewayDeployment +: scalambdaModule.lambdaPermissions

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
