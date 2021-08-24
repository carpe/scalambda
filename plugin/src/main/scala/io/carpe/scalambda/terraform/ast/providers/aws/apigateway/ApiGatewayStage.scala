package io.carpe.scalambda.terraform.ast.providers.aws.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TLine.TInline
import io.carpe.scalambda.terraform.ast.props.{TDynamicBlock, TValue}
import io.carpe.scalambda.terraform.ast.props.TValue.{InfixExpression, TArray, TBool, TIf, TLiteral, TNumber, TResourceRef, TString, TVariableRef}

case class ApiGatewayStage(restApi: ApiGateway, apiGatewayDeployment: ApiGatewayDeployment, xrayToggle: TVariableRef, accessLogGroupArn: TVariableRef, accessLogFormat: TVariableRef) extends Resource {
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
    "xray_tracing_enabled" -> xrayToggle
  )

  /**
   * Dynamic blocks. Reference: https://www.terraform.io/docs/language/expressions/dynamic-blocks.html
   */
  override def dynamicBlocks: Seq[TDynamicBlock] = Seq({
    // check if access log group is an empty string
    val accessLogGroupArnIsSet = InfixExpression(accessLogGroupArn, "==", TString(""))
    val forEachPredicate = TIf(accessLogGroupArnIsSet, ifTrue = TLiteral("toset([1])"), ifFalse = TLiteral("toset([])"))

    TDynamicBlock("access_log_settings", forEach = forEachPredicate, props = Map(
      "destination_arn" -> accessLogGroupArn,
      "format" -> accessLogFormat
    ))
  })
}

