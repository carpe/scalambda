package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.providers.aws.s3.S3Bucket
import org.scalatest.flatspec.AnyFlatSpec

class S3BucketSpec extends AnyFlatSpec {
  "S3Bucket" should "be a serializable terraform resource" in {
    val actual = S3Bucket("myS3Bucket").toString

    val expected =
      """resource "aws_s3_bucket" "my_s3bucket" {
        |  bucket = "my-s3bucket-${terraform.workspace}"
        |  force_destroy = true
        |}
        |""".stripMargin

    assert(actual === expected)
  }
}
