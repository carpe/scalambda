package io.carpe.scalambda.terraform.ast.module

import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.TerraformFile
import io.carpe.scalambda.terraform.ast.resources.{LambdaFunction, S3Bucket, S3BucketItem}

case class ScalambdaModule(lambdas: Seq[LambdaFunction], s3Buckets: Seq[S3Bucket], s3BucketItems: Seq[S3BucketItem], variables: Seq[Variable[_]])

object ScalambdaModule {

  def write(scalambdaModule: ScalambdaModule, rootPath: String): Unit = {
    val lambdasFile = TerraformFile(scalambdaModule.lambdas, "lambdas.tf")
    val s3File = TerraformFile(scalambdaModule.s3Buckets ++ scalambdaModule.s3BucketItems, "s3.tf")
    val variablesFile = TerraformFile(scalambdaModule.variables, "variables.tf")

    val allFiles = Seq(lambdasFile, s3File, variablesFile)
    TerraformFile.writeFiles(allFiles, rootPath)
  }
}
