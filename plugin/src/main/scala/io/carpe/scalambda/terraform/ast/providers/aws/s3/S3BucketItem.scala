package io.carpe.scalambda.terraform.ast.providers.aws.s3

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TResourceRef, TString}

case class S3BucketItem(s3Bucket: S3Bucket, name: String, key: String, source: String, etag: TValue) extends Resource {
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
    "etag" -> etag
  )
}