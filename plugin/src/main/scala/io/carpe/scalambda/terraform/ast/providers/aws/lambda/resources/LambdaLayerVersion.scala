package io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.s3.S3BucketItem

case class LambdaLayerVersion(layerName: String, s3BucketItem: S3BucketItem) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_lambda_layer_version"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "dependency_layer"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "layer_name" -> TString(layerName),
    "description" -> TString("thick dependency layer created by Scalambda"),
    "s3_bucket" -> TResourceRef(s3BucketItem, "bucket"),
    "s3_key" -> TResourceRef(s3BucketItem, "key"),
    "s3_object_version" -> TResourceRef(s3BucketItem, "version_id"),
    "compatible_runtimes" -> TArray(
      TString("java8"),
      TString("java11")
    )
  )
}
