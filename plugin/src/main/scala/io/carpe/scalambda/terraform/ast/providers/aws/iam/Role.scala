package io.carpe.scalambda.terraform.ast.providers.aws.iam

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{THeredoc, TString}

case class Role(namePrefix: String, policy: String, description: String) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override def resourceType: String = "aws_iam_role"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = StringUtils.toSnakeCase(namePrefix)

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "name_prefix" -> TString(namePrefix),
    "assume_role_policy" -> THeredoc(policy)
  )
}
