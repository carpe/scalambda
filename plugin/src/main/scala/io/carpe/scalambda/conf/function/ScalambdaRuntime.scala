package io.carpe.scalambda.conf.function

sealed trait ScalambdaRuntime {
  def identifier: String
}

object ScalambdaRuntime {

  case object Java8 extends ScalambdaRuntime {
    override def identifier: String = "java8"
  }
  case object Java11 extends ScalambdaRuntime {
    override def identifier: String = "java11"
  }
  case object GraalNative extends ScalambdaRuntime {
    override def identifier: String = "provided"
  }
}
