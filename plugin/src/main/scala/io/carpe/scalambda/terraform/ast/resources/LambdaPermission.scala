package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TResourceRef, TString}

case class LambdaPermission(lambda: LambdaFunction, apiGateway: ApiGateway) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_permission")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${lambda.name}_api_permission"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "statement_id" -> TString("Allow${title(aws_lambda_function." + lambda.name + ".name)}InvokeBy${title(aws_api_gateway_rest_api." + apiGateway.name + ".name)}"),
    "action" -> TString("lambda:InvokeFunction"),
    "function_name" -> TResourceRef("aws_lambda_function", lambda.name, "name"),
    "principal" -> TString("apigateway.amazonaws.com"),
    "qualifier" -> TResourceRef("aws_lambda_function", lambda.name, "qualifier"),
    "source_arn" -> TString("${aws_api_gateway_rest_api." + apiGateway.name + ".execution_arn}*/*")
  )
}
