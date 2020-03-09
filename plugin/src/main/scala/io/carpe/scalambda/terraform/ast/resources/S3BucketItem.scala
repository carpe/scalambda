package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.data.ArchiveFile
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TDataRef, TResourceRef, TString}

case class S3BucketItem(s3Bucket: S3Bucket, name: String, key: String, source: ArchiveFile) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_s3_bucket_object")

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "key" -> TString(key),
    "bucket" -> TResourceRef("aws_s3_bucket", s3Bucket.name, "id"),
    "source" -> TDataRef(source.dataType, source.name, "output_path")
  )
}