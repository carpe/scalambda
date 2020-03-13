package io.carpe.scalambda.conf.function

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TString, TVariableRef}

sealed trait FunctionRoleSource {

  /**
   * The value that the function role will be will be set to
   * @param function to infer name from
   */
  def asTFValue(function: ScalambdaFunction.ProjectFunction): TValue
}

object FunctionRoleSource {

  /**
   * Use this if you want your function's role to be provided to you via an input variable that will be generated in the
   * resulting Terraform module.
   */
  case object FromVariable extends FunctionRoleSource {
    override def asTFValue(scalambdaFunction: ScalambdaFunction.ProjectFunction): TValue = TVariableRef(variableName(scalambdaFunction))
    def variableName(scalambdaFunction: ScalambdaFunction.ProjectFunction): String = s"${scalambdaFunction.terraformLambdaResourceName}_role_arn"
  }

  case class StaticArn(roleArn: String) extends FunctionRoleSource {
    /**
     * The value that the function name will be set to
     *
     * @param function to infer name from
     */
    override def asTFValue(function: ScalambdaFunction.ProjectFunction): TValue = TString(roleArn)
  }

}
