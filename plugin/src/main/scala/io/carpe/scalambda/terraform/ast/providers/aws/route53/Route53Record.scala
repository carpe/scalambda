package io.carpe.scalambda.terraform.ast.providers.aws.route53

import io.carpe.scalambda.terraform.ast.Definition.{Resource, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway.ApiGatewayDomainName

case class Route53Record(name: String, apiGatewayDomainName: ApiGatewayDomainName, zoneId: TValue, toggle: Variable[TBool]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override def resourceType: String = "aws_route53_record"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "count" -> TLiteral(s"var.${toggle.name} ? 1 : 0"),
    "zone_id" -> zoneId,
    "name" -> TResourceRef(apiGatewayDomainName, "domain_name"),
    "type" -> TString("A"),
    "alias" -> TBlock(
      "name" -> TResourceRef(apiGatewayDomainName, "cloudfront_domain_name"),
      "zone_id" -> TResourceRef(apiGatewayDomainName, "cloudfront_zone_id"),
      "evaluate_target_health" -> TBool(false)
    )
  )
}