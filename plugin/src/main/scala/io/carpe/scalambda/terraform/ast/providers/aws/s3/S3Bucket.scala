package io.carpe.scalambda.terraform.ast.providers.aws.s3

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag

case class S3Bucket(bucketName: String, billingTags: Seq[BillingTag], additionalBillingTagsVariable: TVariableRef) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_s3_bucket"

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
    "bucket" -> TString(name.replace('_', '-') + "-${terraform.workspace}"),
    "force_destroy" -> TBool(true),
    // billing tags is set to a function that merges both the billing tags provided by the user's configuration in sbt,
    // as well as the ones provided by the tf variable
    "tags" -> TFunctionInvocation(functionName = "merge", arguments = Seq(
      TObject(
        billingTags.map(billingTag => {
          billingTag.name -> TString(billingTag.value)
        }): _*),
      additionalBillingTagsVariable
    )),
  )
}
