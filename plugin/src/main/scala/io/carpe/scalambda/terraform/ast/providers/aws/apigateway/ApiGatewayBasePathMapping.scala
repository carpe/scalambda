package io.carpe.scalambda.terraform.ast.providers.aws.apigateway

import io.carpe.scalambda.terraform.ast.Definition.{Resource, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TLiteral, TResourceRef}

case class ApiGatewayBasePathMapping(api: ApiGateway, stage: ApiGatewayStage, domainName: ApiGatewayDomainName, toggle: Variable[TBool]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_api_gateway_base_path_mapping"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "api_path_mapping"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "count" -> TLiteral(s"var.${toggle.name} ? 1 : 0"),
    "domain_name" -> TResourceRef(domainName, "domain_name"),
    "stage_name" -> TResourceRef(stage, "stage_name"),
    "api_id" -> TResourceRef(api, "id")
  )
}
