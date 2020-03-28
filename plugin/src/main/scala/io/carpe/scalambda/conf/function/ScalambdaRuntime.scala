package io.carpe.scalambda.conf.function

sealed trait ScalambdaRuntime {
  def version: String
}

object ScalambdaRuntime {
  case object Java8 extends ScalambdaRuntime {
    override def version: String = "java8"
  }
  case object Java11 extends ScalambdaRuntime {
    override def version: String = "java11"
  }
}
