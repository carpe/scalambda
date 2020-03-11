package io.carpe.scalambda.terraform.ast.module

import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.TerraformFile
import io.carpe.scalambda.terraform.ast.data.{ArchiveFile, TemplateFile}
import io.carpe.scalambda.terraform.ast.resources.{ApiGateway, ApiGatewayDeployment, LambdaFunction, LambdaLayerVersion, LambdaPermission, S3Bucket, S3BucketItem}

case class ScalambdaModule( // lambda resources
                            lambdas: Seq[LambdaFunction],
                            lambdaDependencyLayer: LambdaLayerVersion,
                            s3Buckets: Seq[S3Bucket],
                            s3BucketItems: Seq[S3BucketItem],
                            sources: Seq[ArchiveFile],

                            // api gateway resources
                            apiGateway: Option[ApiGateway],
                            lambdaPermissions: Seq[LambdaPermission],
                            swaggerTemplate: Option[TemplateFile],
                            apiGatewayDeployment: Option[ApiGatewayDeployment],

                            // other
                            variables: Seq[Variable[_]]
                          )

object ScalambdaModule {

  def write(scalambdaModule: ScalambdaModule, rootPath: String): Unit = {
    val lambdasFile = TerraformFile(scalambdaModule.lambdaDependencyLayer +: scalambdaModule.lambdas, "lambdas.tf")
    val s3File = TerraformFile(scalambdaModule.s3Buckets ++ scalambdaModule.sources ++ scalambdaModule.s3BucketItems, "s3.tf")
    val variablesFile = TerraformFile(scalambdaModule.variables, "variables.tf")
    val coreLambdaFiles = Seq(lambdasFile, s3File, variablesFile)

    val apiFiles = scalambdaModule match {
      case ScalambdaModule(_, _, _, _, _, Some(apiGateway), _, Some(swaggerTemplate), Some(apiGatewayDeployment), _) =>
        Seq(
          TerraformFile(apiGateway +: swaggerTemplate +: apiGatewayDeployment +: scalambdaModule.lambdaPermissions, "api.tf")
        )
      case _ =>
        Seq.empty
    }

    TerraformFile.writeFiles(coreLambdaFiles ++ apiFiles, rootPath)

    println(s"${coreLambdaFiles.map(_.definitions.size).sum} Terraform definitions were generated. They have been written to: ${rootPath}")
  }
}
