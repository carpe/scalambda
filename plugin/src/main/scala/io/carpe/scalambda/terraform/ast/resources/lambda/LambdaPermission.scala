package io.carpe.scalambda.terraform.ast.resources.lambda

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.resources.apigateway.ApiGateway

case class LambdaPermission(alias: LambdaFunctionAlias, apiGateway: ApiGateway) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_permission")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${alias.function.name}_api_permission"

  val statementId: String = {
    "Allow${title(aws_lambda_function." + alias.function.name + ".function_name)}InvokeBy${title(aws_api_gateway_rest_api." + apiGateway.name + ".name)}"
  }

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "statement_id" -> TString(statementId),
    "action" -> TString("lambda:InvokeFunction"),
    "function_name" -> TResourceRef("aws_lambda_function", alias.function.name, "function_name"),
    "qualifier" -> TResourceRef("aws_lambda_alias", alias.name, "name"),
    "principal" -> TString("apigateway.amazonaws.com"),
    "source_arn" -> TString("${aws_api_gateway_rest_api." + apiGateway.name + ".execution_arn}/*/*")
  )
}
