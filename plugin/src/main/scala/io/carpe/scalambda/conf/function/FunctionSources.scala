package io.carpe.scalambda.conf.function

import java.io.File

case class FunctionSources(dependencyJar: Option[File], functionJar: Option[File], nativeImage: Option[File])

object FunctionSources {

  lazy val empty: FunctionSources = FunctionSources(None, None, None)

}

