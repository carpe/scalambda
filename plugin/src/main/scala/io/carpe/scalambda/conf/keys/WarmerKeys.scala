package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function
import io.carpe.scalambda.conf.function.WarmerConfig
import io.circe.Json

trait WarmerKeys {

  /**
   * Uses provisioned concurrency to keep your function slightly warmer. Not recommended for most use cases, as a
   * cloudwatch event (which you can create via [[WarmerConfig.Invocation]]) is a little more straight-forward.
   *
   * @param concurrency to provision
   */
  def ProvisionedConcurrency(concurrency: Int): WarmerConfig.ProvisionedConcurrency = function.WarmerConfig.ProvisionedConcurrency(concurrency)

  /**
   * Invokes your lambda function with the supplied json as input on a set interval in order to keep it warm.
   * @param json to send to function
   */
  def Invocation(json: Json): WarmerConfig.Invocation = function.WarmerConfig.Invocation(json)

  /**
   * Uses Scalambda's built in Warmer handler, which will simply initialize your Function's handler class, without
   * actually triggering the code inside your handler to run.
   *
   * This will cause any `val` fields on your handler to be instantiated, which Provisioned Concurrency will NOT do.
   */
  def NoOp: WarmerConfig.NoOp.type = WarmerConfig.NoOp

}
