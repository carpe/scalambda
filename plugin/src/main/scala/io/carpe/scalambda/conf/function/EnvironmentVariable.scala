package io.carpe.scalambda.conf.function

sealed trait EnvironmentVariable

object EnvironmentVariable {

  case class StaticVariable(key: String, value: String) extends EnvironmentVariable

  case class VariableFromTF(key: String, variableName: String) extends EnvironmentVariable

  @deprecated("use `StaticVariable` instead of `environmentVariable.Static`", since = "5.0.0")
  def Static(key: String, value: String): StaticVariable = StaticVariable.apply(key, value)

  @deprecated("use `VariableFromTF` instead of `environmentVariable.Variable`", since = "5.0.0")
  def Variable(key: String, variableName: String): VariableFromTF = VariableFromTF.apply(key, variableName)

}
