package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TBlock, TBool, TLiteral, TResourceRef, TString}

case class ApiGatewayDeployment(apiGateway: ApiGateway, lambdaPermissions: Seq[LambdaPermission]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_api_gateway_deployment")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "deployment"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "rest_api_id" -> TResourceRef("aws_api_gateway_rest_api", apiGateway.name, "id"),
    "stage_name" -> TLiteral("terraform.workspace"),
    "lifecycle" -> TBlock(
      "create_before_destroy" -> TBool(true)
    ),
    "depends_on" -> TArray(lambdaPermissions.map(lambdaPermission => {
      TString(s"aws_lambda_permission.${lambdaPermission.name}")
    }): _*)
  )

//  resource "aws_api_gateway_deployment" "deployment" {
//    rest_api_id = aws_api_gateway_rest_api.api.id
//    stage_name  = var.stage_name
//
//    lifecycle {
//      create_before_destroy = true
//    }
//
//    depends_on = ["aws_lambda_permission.api_gateway_lambdas_allowance"]
//  }
}
