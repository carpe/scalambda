package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TString}

case class S3Bucket(name: String) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_s3_bucket")

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "bucket" -> TString(name),
    "force_destroy" -> TBool(true)
  )

}
