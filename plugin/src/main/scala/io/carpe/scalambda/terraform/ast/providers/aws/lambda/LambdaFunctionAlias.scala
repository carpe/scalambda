package io.carpe.scalambda.terraform.ast.providers.aws.lambda

import io.carpe.scalambda.terraform.ast.Definition
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TRef
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaFunction

trait LambdaFunctionAlias extends Definition {
  def functionName: TRef
  def invokeArn: TRef
  def qualifier: TRef
}
