package io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TLiteral, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias

/**
 * For now, this class exists solely to allow Scalambda to manage an alias that points to the latest version of the
 * lambda function. This is helpful if you are using a lambda "fan out" and want to insure that the lambdas are only
 * "fanning out" to lambda functions with their same version.
 */
case class LambdaFunctionAliasResource(function: LambdaFunction, aliasName: String, description: String) extends Resource with LambdaFunctionAlias {

  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override lazy val resourceType: String = "aws_lambda_alias"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${function.name}_${StringUtils.toSnakeCase({
    try {
      aliasName.substring(0, 16)
    } catch {
      case e: IndexOutOfBoundsException =>
        aliasName
    }
  })}"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "name" -> TString(aliasName),
    "description" -> TString(description),
    "function_name" -> TResourceRef(function, "function_name"),
    "function_version" -> TResourceRef(function, "version"),
    "depends_on" -> TArray(
      TLiteral(s"aws_lambda_function.${function.name}")
    )
  )

  override def invokeArn: TValue.TRef = TResourceRef(this, "invoke_arn")

  override def functionName: TValue.TRef = TResourceRef(this, "function_name")

  override def qualifier: TValue.TRef = TResourceRef(this, "name")

  override def qualifiedArn: TValue = TString("${" + TResourceRef(function, "arn").asInterpolatedRef + s"}:${aliasName}")

  /**
   * @return Example: MyFunctionName
   */
  override def approximateFunctionName: String = function.scalambdaFunction.approximateFunctionName

  override def swaggerVariableName: String = function.swaggerVariableName
}
