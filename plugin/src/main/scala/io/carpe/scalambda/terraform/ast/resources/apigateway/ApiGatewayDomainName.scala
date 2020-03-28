package io.carpe.scalambda.terraform.ast.resources.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TString, TVariableRef}

case class ApiGatewayDomainName(domainName: String) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_api_gateway_domain_name")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "api_domain"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "domain_name" -> TString(domainName),
    "certificate_arn" -> TVariableRef("certificate_arn")
  )
}
