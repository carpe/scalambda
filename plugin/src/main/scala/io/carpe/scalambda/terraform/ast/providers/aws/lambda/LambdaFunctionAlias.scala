package io.carpe.scalambda.terraform.ast.providers.aws.lambda

import io.carpe.scalambda.terraform.ast.Definition
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TRef

trait LambdaFunctionAlias extends Definition {

  def swaggerVariableName: String

  /**
   * @return Example: MyFunctionName
   */
  def approximateFunctionName: String

  /**
   * @return Terraform reference to function name
   */
  def functionName: TRef

  /**
   * @return Terraform reference to function invoke arn
   */
  def invokeArn: TRef

  /**
   * @return Terraform reference to function qualified arn
   */
  def qualifiedArn: TValue

  /**
   * @return Terraform reference to the qualifier for this alias
   */
  def qualifier: TRef
}
