package io.carpe.scalambda.terraform.ast.resources.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TResourceRef

case class ApiGatewayBasePathMapping(api: ApiGateway, apiGatewayDeployment: ApiGatewayDeployment, domainName: ApiGatewayDomainName) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_api_gateway_base_path_mapping")

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
    "domain_name" -> TResourceRef("aws_api_gateway_domain_name", domainName.name, "domain_name"),
    "stage_name" -> TResourceRef("aws_api_gateway_deployment", apiGatewayDeployment.name, "stage_name"),
    "api_id" -> TResourceRef("aws_api_gateway_rest_api", api.name, "id")
  )
}
