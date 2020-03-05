package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TResourceRef, TString}

case class S3BucketItem(s3Bucket: S3Bucket, key: String, source: String) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_s3_bucket_object")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = key

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "key" -> TString(key),
    "bucket" -> TResourceRef("aws_s3_bucket", s3Bucket.name, "id"),
    "source" -> TString(source)
  )
}