package io.carpe.scalambda.terraform.openapi.resourcemethod

import io.carpe.scalambda.terraform.openapi.ResourceMethod
import io.circe.{Encoder, Json}

/**
 * Reference to security method for a [[ResourceMethod]]
 * @param name of security (usually some reference)
 */
case class Security(name: String)

object Security {

  implicit val encoder: Encoder[Security] = (security: Security) => Json.obj(
    (security.name, Json.fromValues(List.empty))
  )
}
