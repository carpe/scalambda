package io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString

case class LambdaPermission(lambdaResourceName: String, statementId: String, principal: String, functionName: TValue, qualifier: TValue, sourceArn: TValue) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_lambda_permission"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${lambdaResourceName}_api_permission"
  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "statement_id" -> TString(statementId),
    "action" -> TString("lambda:InvokeFunction"),
    "function_name" -> functionName,
    "principal" -> TString(principal),
    "qualifier" -> qualifier,
    "source_arn" -> sourceArn
  )
}

