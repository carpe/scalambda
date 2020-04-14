package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.TerraformBehaviors
import io.carpe.scalambda.terraform.ast.providers.aws.s3.S3Bucket
import org.scalatest.flatspec.AnyFlatSpec

class S3BucketSpec extends AnyFlatSpec with TerraformBehaviors{
  "S3Bucket" should behave like printableTerraform(S3Bucket("myS3Bucket"), {
    """resource "aws_s3_bucket" "my_s3bucket" {
      |  bucket = "my-s3bucket-${terraform.workspace}"
      |  force_destroy = true
      |}""".stripMargin
  })
}
