package io.carpe.scalambda.terraform.ast.providers.aws.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TLiteral, TResourceRef}

case class ApiGatewayStage(restApi: ApiGateway, apiGatewayDeployment: ApiGatewayDeployment, isXrayEnabled: Boolean) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_api_gateway_stage"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "active_stage"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "stage_name" -> TLiteral("terraform.workspace"),
    "rest_api_id" -> TResourceRef(restApi, "id"),
    "deployment_id" -> TResourceRef(apiGatewayDeployment, "id"),
    "xray_tracing_enabled" -> TBool(isXrayEnabled)
  )
}

