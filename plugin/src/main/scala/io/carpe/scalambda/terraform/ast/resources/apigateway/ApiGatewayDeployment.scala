package io.carpe.scalambda.terraform.ast.resources.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.resources.lambda.LambdaPermission

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
    "stage_name" -> TString("intermediate"),
    "lifecycle" -> TBlock(
      "create_before_destroy" -> TBool(true)
    ),
    "depends_on" -> TArray(lambdaPermissions.map(lambdaPermission => {
      TLiteral(s"aws_lambda_permission.${lambdaPermission.name}")
    }): _*)
  )

}
