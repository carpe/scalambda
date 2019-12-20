package io.carpe.scalambda.response

import io.circe.Encoder

case class APIGatewayProxyResponse[T](statusCode: Int,
                                      headers: Map[String, String] = Map.empty,
                                      body: Option[Either[ApiError, T]] = None,
                                      isBase64Encoded: Boolean = false
                                     )

object APIGatewayProxyResponse {


  implicit def encode[T](implicit typeEncoder: Encoder[T]): Encoder[APIGatewayProxyResponse[T]] = {
    import io.circe.generic.semiauto._
    import io.circe.syntax._

    implicit val encodeErrorOrResult: Encoder[Either[ApiError, T]] =
      Encoder.instance(_.fold(_.asJson, _.asJson))

    deriveEncoder[APIGatewayProxyResponse[T]]
  }
}
