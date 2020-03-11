package io.carpe.scalambda.conf.function

case class FunctionConf(memory: Int = 1536, timeout: Int)

object FunctionConf {
  lazy val carpeDefault: FunctionConf = FunctionConf(
    timeout = 60 * 15
  )
}