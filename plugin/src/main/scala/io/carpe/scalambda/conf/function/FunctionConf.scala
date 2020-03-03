package io.carpe.scalambda.conf.function

case class FunctionConf(iamRole: String, memory: Int = 1536, timeout: Int)

object FunctionConf {
  lazy val carpeDefault: FunctionConf = FunctionConf(
    iamRole = "arn:aws:iam::120864075170:role/lambda_basic_execution",
    timeout = 60 * 15
  )
}