package io.carpe.scalambda.terraform.ast.resources.lambda

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.resources.apigateway.ApiGateway

case class LambdaPermission(lambdaResourceName: String, statementId: String, ref: String => TValue, apiGateway: ApiGateway) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_permission")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${lambdaResourceName}_api_permission"
  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "statement_id" -> TString(statementId),
    "action" -> TString("lambda:InvokeFunction"),
    "function_name" -> ref("function_name"),
    "principal" -> TString("apigateway.amazonaws.com"),
    "source_arn" -> TString("${aws_api_gateway_rest_api." + apiGateway.name + ".execution_arn}/*/*")
  )
}

