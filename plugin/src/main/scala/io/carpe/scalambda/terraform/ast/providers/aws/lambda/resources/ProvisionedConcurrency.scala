package io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources

import io.carpe.scalambda.terraform.ast.Definition.{Resource, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TBool, TIf, TLiteral, TNumber, TResourceRef}

case class ProvisionedConcurrency(functionAlias: LambdaFunctionAliasResource, enableWarmers: Variable[TBool], desiredConcurrency: Int) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_lambda_provisioned_concurrency_config"

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
    "function_name" -> TResourceRef(functionAlias, "function_name"),
    "provisioned_concurrent_executions" -> TIf(enableWarmers.ref, TNumber(desiredConcurrency), TNumber(0)),
    "qualifier" -> TResourceRef(functionAlias, "name"),
    "depends_on" -> TArray(
      TLiteral(s"aws_lambda_alias.${functionAlias.name}")
    )
  )
}