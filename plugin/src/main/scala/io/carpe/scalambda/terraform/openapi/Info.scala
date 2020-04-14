package io.carpe.scalambda.terraform.openapi

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class Info(version: String, title: String)

object Info {
  lazy implicit val encoder: Encoder[Info] = deriveEncoder[Info]
}
