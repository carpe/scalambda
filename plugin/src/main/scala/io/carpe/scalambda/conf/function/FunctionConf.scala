package io.carpe.scalambda.conf.function

case class FunctionConf(memory: Int = 1536, timeout: Int)

object FunctionConf {
  lazy val default: FunctionConf = FunctionConf(
    timeout = 60 * 15
  )

  lazy val apiDefault: FunctionConf = FunctionConf(
    timeout = 30
  )
}