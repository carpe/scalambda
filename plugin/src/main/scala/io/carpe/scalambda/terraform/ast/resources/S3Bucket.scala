package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TString}

case class S3Bucket(bucketName: String) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_s3_bucket")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = StringUtils.toSnakeCase(bucketName).replace('-', '_')

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "bucket" -> TString(name.replace('_', '-')),
    "force_destroy" -> TBool(true)
  )
}