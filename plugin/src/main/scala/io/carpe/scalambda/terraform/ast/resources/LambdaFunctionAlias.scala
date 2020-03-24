package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TResourceRef, TString}

/**
 * For now, this class exists solely to allow Scalambda to manage an alias that points to the latest version of the
 * lambda function. This is helpful if you are using a lambda "fan out" and want to insure that the lambdas are only
 * "fanning out" to lambda functions with their same version.
 */
case class LambdaFunctionAlias(function: LambdaFunction, aliasName: String) extends Resource {

  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_alias")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${function.name}_${StringUtils.toSnakeCase(aliasName)}"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "name" -> TString(aliasName),
    "description" -> TString("Managed by Scalambda"),
    "function_name" -> TResourceRef("aws_lambda_function", function.name, "name"),
    "function_version" -> TResourceRef("aws_lambda_function", function.name, "version"),
  )
}
