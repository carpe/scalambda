package io.carpe.scalambda.terraform.ast.providers.aws.s3

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TObject, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag

case class S3BucketItem(s3Bucket: S3Bucket, name: String, key: String, source: String, etag: TValue, billingTags: Seq[BillingTag]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_s3_bucket_object"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "key" -> TString(key),
    "bucket" -> TResourceRef(s3Bucket, "id"),
    "source" -> TString("${path.module}/" + source),
    "etag" -> etag,
    "tags" -> TObject(
      billingTags.map(billingTag => {
        billingTag.name -> TString(billingTag.value)
      })
    : _*)
  )
}