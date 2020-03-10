package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TDataRef, TLiteral, TResourceRef, TString}

case class S3BucketItem(s3Bucket: S3Bucket, name: String, key: String, source: String, etag: String) extends Resource {
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
    "source" -> TString("${path.module}/" + source),
    "etag" -> TLiteral("""filemd5("""" + "${path.module}/" + etag + """")""")
  )
}