package io.carpe.scalambda.conf.function

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString

sealed trait FunctionNaming {
  def asTValue(function: ScalambdaFunction.DefinedFunction): TValue
}

object FunctionNaming {

  def inferLambdaName(functionClasspath: String): String = {
    functionClasspath.split('.').last.split(":").head
  }

  /**
   * Sets the Lambda Function's name to be the name of the class that the lambda function is derived from, with the
   * current terraform workspace appended on the end.
   *
   * Examples:
   *
   * - Example #1
   * Function Handler: my.lambda.FlyPlane::handler
   * Terraform Workspace: dev
   * Result: FlyPlaneDev
   *
   * - Example #2
   * Function Handler: my.lambda.CreateCar::handler
   * Terraform Workspace: default
   * Result: CreateCarDefault
   *
   */
  case object WorkspaceBased extends FunctionNaming {
    override def asTValue(function: ScalambdaFunction.DefinedFunction): TValue = {
      val functionName = inferLambdaName(function.handlerPath)
      TString(functionName + "${title(terraform.workspace)}")
    }
  }

  /**
   * Sets the Lambda Function's name to some static value that you manage
   * @param name for the lambda function
   */
  case class Static(name: String) extends FunctionNaming {
    override def asTValue(function: ScalambdaFunction.DefinedFunction): TValue = TString(name)
  }
}