package io.carpe.scalambda.conf.function

sealed trait ArchitectureConfig {
  def identifier: String
}

object ArchitectureConfig {

  case object X8664 extends ArchitectureConfig {
    override def identifier: String = "x86_64"
  }
  case object Arm64 extends ArchitectureConfig {
    override def identifier: String = "arm64"
  }
}