package io.carpe.scalambda.native.views

import io.circe.{Encoder, Json}

case class LambdaError(errorType: String, errorMessage: String)

object LambdaError {

  implicit val encoder: Encoder[LambdaError] = new Encoder[LambdaError] {
    override def apply(a: LambdaError): Json = Json.obj(
      "errorType" -> Json.fromString(a.errorType),
      "errorMessage" -> Json.fromString(a.errorMessage)
    )
  }
}
