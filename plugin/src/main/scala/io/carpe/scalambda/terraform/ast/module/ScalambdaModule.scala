package io.carpe.scalambda.terraform.ast.module

import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}
import io.carpe.scalambda.terraform.ast.providers.terraform.data.ArchiveFile
import io.carpe.scalambda.terraform.ast.{Definition, TerraformFile}
import io.carpe.scalambda.terraform.writer.TerraformPrinter

case class ScalambdaModule( // lambda resources
                            lambdaResources: Seq[Definition],
                            lambdaWarmingResources: Seq[Definition],
                            s3Buckets: Seq[S3Bucket],
                            s3BucketItems: Seq[S3BucketItem],
                            sources: Seq[ArchiveFile],

                            // api gateway resources
                            apiGatewayResources: Seq[Definition],

                            // other
                            variables: Seq[Variable[_]],
                            outputs: Seq[Output[_]]
                          )

object ScalambdaModule {

  def write(scalambdaModule: ScalambdaModule, rootPath: String): Unit = {
    val lambdasFile = TerraformFile(scalambdaModule.lambdaResources, "lambdas.tf")
    val warmersFile = TerraformFile(scalambdaModule.lambdaWarmingResources, "warming.tf")
    val s3File = TerraformFile(scalambdaModule.s3Buckets ++ scalambdaModule.sources ++ scalambdaModule.s3BucketItems, "s3.tf")
    val variablesAndOutputsFile = TerraformFile(scalambdaModule.variables ++ scalambdaModule.outputs, "io.tf")
    val coreLambdaFiles = Seq(lambdasFile, warmersFile, s3File, variablesAndOutputsFile)

    val apiFiles = scalambdaModule.apiGatewayResources match {
      case Seq() =>
        Seq()
      case _ =>
        Seq(TerraformFile(scalambdaModule.apiGatewayResources, "api.tf"))
    }

    (coreLambdaFiles ++ apiFiles).foreach(file => {
      TerraformPrinter.writeFile(rootPath, file)
    })

    println(s"${coreLambdaFiles.map(_.definitions.size).sum} Terraform definitions were generated. They have been written to: ${rootPath}")
  }
}
