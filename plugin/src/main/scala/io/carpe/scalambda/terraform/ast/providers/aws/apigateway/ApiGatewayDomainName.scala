package io.carpe.scalambda.terraform.ast.providers.aws.apigateway

import io.carpe.scalambda.terraform.ast.Definition.{Resource, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TLiteral, TString, TVariableRef}

case class ApiGatewayDomainName(name: String, domainName: TValue, certificateArn: TValue, toggle: Variable[TBool]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_api_gateway_domain_name"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "count" -> TLiteral(s"var.${toggle.name} ? 1 : 0"),
    "domain_name" -> domainName,
    "certificate_arn" -> certificateArn
  )
}
