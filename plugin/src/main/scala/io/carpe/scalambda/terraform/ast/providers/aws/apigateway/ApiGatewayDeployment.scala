package io.carpe.scalambda.terraform.ast.providers.aws.apigateway

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaPermission

case class ApiGatewayDeployment(apiGateway: ApiGateway, lambdaPermissions: Seq[LambdaPermission]) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_api_gateway_deployment"

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
    "rest_api_id" -> TResourceRef(apiGateway, "id"),
    "stage_name" -> TString("intermediate"),
    "lifecycle" -> TBlock(
      "create_before_destroy" -> TBool(true)
    ),
    "depends_on" -> TArray(lambdaPermissions.map(lambdaPermission => {
      TLiteral(s"aws_lambda_permission.${lambdaPermission.name}")
    }): _*)
  )

}
