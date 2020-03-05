package io.carpe.scalambda.conf.function

sealed trait FunctionSource

object FunctionSource {
  case class LocalSources(path: String) extends FunctionSource
}
