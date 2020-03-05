package io.carpe.scalambda.conf.function

import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TVariableRef

sealed trait FunctionRoleSource {
  def asTFValue: TValue
}

object FunctionRoleSource {
  case class FromVariable(functionName: String, description: String) extends FunctionRoleSource {
    override def asTFValue: TValue = TVariableRef(variableName)
    lazy val variableName = s"${functionName}_role_arn"
  }
}
