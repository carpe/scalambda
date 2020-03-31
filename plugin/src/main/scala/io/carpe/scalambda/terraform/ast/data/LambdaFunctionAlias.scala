package io.carpe.scalambda.terraform.ast.data

import io.carpe.scalambda.conf.ScalambdaFunction.ReferencedFunction
import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString

case class LambdaFunctionAlias(referencedFunction: ReferencedFunction) extends Data {
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
}

object LambdaFunctionAlias {
  type DataLambdaFunctionAlias = LambdaFunctionAlias
}