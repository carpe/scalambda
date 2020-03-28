package io.carpe.scalambda.conf.function

case class RuntimeConfig(memory: Int, timeout: Int, runtime: ScalambdaRuntime)

object RuntimeConfig {
  lazy val default: RuntimeConfig = RuntimeConfig(
    memory = 1536,
    timeout = 60 * 15,
    runtime = ScalambdaRuntime.Java8
  )

  lazy val apiDefault: RuntimeConfig = RuntimeConfig.default.copy(timeout = 30)
}