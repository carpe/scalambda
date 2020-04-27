package io.carpe.scalambda.terraform.ast.providers.aws.lambda.data

import io.carpe.scalambda.conf.ScalambdaFunction.ReferencedFunction
import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TDataRef, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias

case class LambdaFunctionAliasData(referencedFunction: ReferencedFunction) extends Data with LambdaFunctionAlias {
  /**
   * Examples: "aws_lambda_function" "template_file"
   *
   * @return
   */
  override def dataType: String = "aws_lambda_alias"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = referencedFunction.terraformLambdaResourceName

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "function_name" -> TString(referencedFunction.functionName),
    "name" -> TString(referencedFunction.qualifier),
  )

  override def invokeArn: TDataRef = TDataRef(this, "invoke_arn")

  override def functionName: TValue.TRef = TDataRef(this, "function_name")

  override def qualifier: TValue.TRef = TDataRef(this, "name")

  override def qualifiedArn: TValue.TRef = TDataRef(this, "in")

  /**
   * @return Example: MyFunctionName
   */
  override def approximateFunctionName: String = referencedFunction.functionName

  override def swaggerVariableName: String = referencedFunction.swaggerVariableName

}