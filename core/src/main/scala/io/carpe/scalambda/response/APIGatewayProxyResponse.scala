package io.carpe.scalambda.response

import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.api.conf.ApiBootstrap
import io.circe.{Encoder, Json}

sealed trait APIGatewayProxyResponse[+T]

object APIGatewayProxyResponse {

  case class Empty( statusCode: Int,
                    headers: Map[String, String] = Map.empty,
                    isBase64Encoded: Boolean = false
                  ) extends APIGatewayProxyResponse[Nothing]

  case class WithError( headers: Map[String, String] = Map.empty,
                        err: ApiErrors,
                        isBase64Encoded: Boolean = false
                      ) extends APIGatewayProxyResponse[Nothing]

  case class WithBody[T]( statusCode: Int,
                          headers: Map[String, String] = Map.empty,
                          body: T,
                          isBase64Encoded: Boolean = false
                        )(implicit val bodyEncoder: Encoder[T]) extends APIGatewayProxyResponse[T] {
    protected[response] def bodyAsString: String = Scalambda.encode(body)(bodyEncoder)
  }

  implicit def encoder[T](implicit bootstrap: ApiBootstrap[_]): Encoder[APIGatewayProxyResponse[T]] = {
    case Empty(statusCode, headers, isBase64Encoded) =>
      Json.obj(
        ("statusCode", Json.fromInt(statusCode)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )

    case WithError(headers, err, isBase64Encoded) =>
      // AWS wants the response body to always be a string, so we encode it first
      val errAsString = bootstrap.errorsEncoder(err).noSpaces

      Json.obj(
        ("statusCode", Json.fromInt(err.errors.head.httpStatus)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("body", Json.fromString(errAsString)),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )

    case wb@WithBody(statusCode, headers, body, isBase64Encoded) =>
      // AWS wants the response body to always be a string, so we encode it first
      val bodyAsString: String = wb.bodyAsString

      Json.obj(
        ("statusCode", Json.fromInt(statusCode)),
        ("headers", Json.fromFields(headers.map({ case (k, v) => (k, Json.fromString(v)) }))),
        ("body", Json.fromString(bodyAsString)),
        ("isBase64Encoded", Json.fromBoolean(isBase64Encoded))
      )
  }
}
