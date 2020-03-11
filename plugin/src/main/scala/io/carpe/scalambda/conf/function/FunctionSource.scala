package io.carpe.scalambda.conf.function

sealed trait FunctionSource

object FunctionSource {
  case object IncludedInModule extends FunctionSource
}
