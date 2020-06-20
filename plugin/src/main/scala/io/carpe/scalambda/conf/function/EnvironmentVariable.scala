package io.carpe.scalambda.conf.function

sealed trait EnvironmentVariable

object EnvironmentVariable {

  case class StaticVariable(key: String, value: String) extends EnvironmentVariable

  case class VariableFromTF(key: String, variableName: String) extends EnvironmentVariable

}
