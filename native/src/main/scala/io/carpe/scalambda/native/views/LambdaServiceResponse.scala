package io.carpe.scalambda.native.views

import io.circe.Encoder

/**
 * This is the response that will be sent back to the AWS Lambda Service
 *
 * @param statusCode for response
 * @param headers for response
 * @param body containing either an error message or the response returned by the user's run logic
 * @param isBase64Encoded whether or not the response is base64 encoded
 */
case class LambdaServiceResponse(
  statusCode: String,
  headers: Map[String, String],
  body: String,
  isBase64Encoded: Boolean
)

object LambdaServiceResponse {
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[LambdaServiceResponse] = deriveEncoder[LambdaServiceResponse]
}
