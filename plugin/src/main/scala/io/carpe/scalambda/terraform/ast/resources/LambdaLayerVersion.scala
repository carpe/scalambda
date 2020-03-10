package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TResourceRef, TString}

case class LambdaLayerVersion(s3BucketItem: S3BucketItem) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_layer_version")

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
    "layer_name" -> TString("scalambda_assembled_dependencies"),
    "description" -> TString("thick dependency layer created by Scalambda"),
    "s3_bucket" -> TResourceRef("aws_s3_bucket_object", s3BucketItem.name, "bucket"),
    "s3_key" -> TResourceRef("aws_s3_bucket_object", s3BucketItem.name, "key"),
    "s3_object_version" -> TResourceRef("aws_s3_bucket_object", s3BucketItem.name, "version_id"),
    "compatible_runtimes" -> TArray(
      TString("java8")
    )
  )
}
