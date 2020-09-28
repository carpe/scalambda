package io.carpe.scalambda.native.views

import io.circe.Encoder

case class LambdaError(errorType: String, errorMessage: String)

object LambdaError {
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[LambdaError] = deriveEncoder[LambdaError]
}
