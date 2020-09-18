package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.TerraformBehaviors
import io.carpe.scalambda.terraform.ast.props.TValue.TVariableRef
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.s3.S3Bucket
import org.scalatest.flatspec.AnyFlatSpec

class S3BucketSpec extends AnyFlatSpec with TerraformBehaviors {

  val billingTagsVariable: TVariableRef = TVariableRef("mah_tawgs")

  "S3Bucket" should behave like printableTerraform(S3Bucket("myS3Bucket", billingTags = Nil, additionalBillingTagsVariable = billingTagsVariable), {
    """resource "aws_s3_bucket" "my_s3bucket" {
      |  bucket = "my-s3bucket-${terraform.workspace}"
      |  force_destroy = true
      |  tags = merge({}, var.mah_tawgs)
      |}""".stripMargin
  })

  "S3Bucket (with billing tags)" should behave like printableTerraform(S3Bucket("myS3Bucket", billingTags = Seq(BillingTag("TheAnswer", "42")), additionalBillingTagsVariable = billingTagsVariable), {
    """resource "aws_s3_bucket" "my_s3bucket" {
      |  bucket = "my-s3bucket-${terraform.workspace}"
      |  force_destroy = true
      |  tags = merge({
      |    TheAnswer = "42"
      |  }, var.mah_tawgs)
      |}""".stripMargin
  })
}
