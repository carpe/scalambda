package io.carpe.scalambda.terraform.ast.resources.lambda

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBlock, TNumber, TResourceRef}
import io.carpe.scalambda.terraform.ast.resources.LambdaFunctionAlias

case class ProvisionedConcurrency(functionAlias: LambdaFunctionAlias, desiredConcurrency: Int) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_provisioned_concurrency_config")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${functionAlias.name}_concurrency"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "function_name" -> TResourceRef("aws_lambda_alias", functionAlias.name, "function_name"),
    "provisioned_concurrent_executions" -> TNumber(desiredConcurrency),
    "qualifier" -> TResourceRef("aws_lambda_alias", functionAlias.name, "name")
  )
}