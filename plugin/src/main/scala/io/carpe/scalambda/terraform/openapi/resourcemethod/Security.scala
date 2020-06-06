package io.carpe.scalambda.terraform.openapi.resourcemethod

import io.carpe.scalambda.terraform.openapi.{ResourceMethod, SecurityDefinition}
import io.circe.{Encoder, Json}

/**
 * Reference to security method for a [[ResourceMethod]]
 * @param name of security (usually some reference)
 */
case class Security(name: String)

object Security {

  def apply(securityDefinition: SecurityDefinition): Security = {
    Security(securityDefinition.authorizerName)
  }

  implicit val encoder: Encoder[Security] = (security: Security) => Json.obj(
    (security.name, Json.fromValues(List.empty))
  )
}
