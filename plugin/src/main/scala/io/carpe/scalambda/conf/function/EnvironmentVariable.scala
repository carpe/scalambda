package io.carpe.scalambda.conf.function

sealed trait EnvironmentVariable

object EnvironmentVariable {

  case class Static(key: String, value: String) extends EnvironmentVariable

  case class Variable(key: String, variableName: String) extends EnvironmentVariable

}
