package io.carpe.scalambda.response

import io.circe.{Encoder, Json}

sealed trait APIGatewayProxyResponse[+T]

object APIGatewayProxyResponse {

  case class Empty( statusCode: Int,
                    headers: Map[String, String] = Map.empty,
                    isBase64Encoded: Boolean = false
                  ) extends APIGatewayProxyResponse[Nothing]

  case class WithError( headers: Map[String, String] = Map.empty,
                        err: ApiError,
                        isBase64Encoded: Boolean = false
                      ) extends APIGatewayProxyResponse[Nothing]

  case class WithBody[T]( statusCode: Int,
                          headers: Map[String, String] = Map.empty,
                          body: T,
                          isBase64Encoded: Boolean = false
                        )(implicit val bodyEncoder: Encoder[T]) extends APIGatewayProxyResponse[T]

  implicit def encoder[T]: Encoder[APIGatewayProxyResponse[T]] = {
    case Empty(statusCode, headers, isBase64Encoded) =>
      Json.obj(
        ("statusCode", Json.fromInt(statusCode)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )

    case WithError(headers, err, isBase64Encoded) =>
      // AWS wants the response body to always be a string, so we encode it first
      val errAsString = ApiError.encoder(err).noSpaces

      Json.obj(
        ("statusCode", Json.fromInt(err.httpStatus)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("body", Json.fromString(errAsString)),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )

    case wb@WithBody(statusCode, headers, body, isBase64Encoded) =>
      // AWS wants the response body to always be a string, so we encode it first
      val bodyAsString = wb.bodyEncoder.apply(wb.body).noSpaces

      Json.obj(
        ("statusCode", Json.fromInt(statusCode)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("body", Json.fromString(bodyAsString)),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )
  }
}
