package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.ScalambdaRuntime
import io.carpe.scalambda.conf.function.ScalambdaRuntime.{Java11, Java8}

trait RuntimeKeys {
  def java8: ScalambdaRuntime = Java8
  def java11: ScalambdaRuntime = Java11
}
