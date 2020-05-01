package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.ScalambdaRuntime
import io.carpe.scalambda.conf.function.ScalambdaRuntime.{Java11, Java8}

trait RuntimeKeys {
  @deprecated(message = "Use Java8 instead", since = "3.0.0")
  lazy val java8: ScalambdaRuntime = Java8

  lazy val Java8: ScalambdaRuntime = ScalambdaRuntime.Java8

  @deprecated(message = "Use Java11 instead", since = "3.0.0")
  def java11: ScalambdaRuntime = Java11

  lazy val Java11: ScalambdaRuntime = ScalambdaRuntime.Java11
}
