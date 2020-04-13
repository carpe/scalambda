package io.carpe.scalambda.conf.function

import io.circe.Json

sealed trait WarmerConfig

object WarmerConfig {

  case class Invocation(json: Json) extends WarmerConfig

  /**
   * Uses Scalambda's built in Warmer handler, which will simply initialize your Function's handler class, without
   * actually triggering the code inside your handler to run.
   *
   * This will cause any `val` fields on your handler to be instantiated, which Provisioned Concurrency will NOT do.
   *
   */
  case object NoOp extends WarmerConfig {
    lazy val json: Json = Json.obj(
      "X-LAMBDA-WARMER" -> Json.fromBoolean(true)
    )
  }

  /**
   * Uses provisioned concurrency to keep your function slightly warmer. Not recommended for most use cases, as a
   * cloudwatch event (which you can create via [[WarmerConfig.Invocation]]) is a little more straight-forward.
 *
   * @param concurrency to provision
   */
  case class ProvisionedConcurrency(concurrency: Int) extends WarmerConfig

  case object Cold extends WarmerConfig
}
