package io.carpe.scalambda

import io.circe.Encoder

case class APIGatewayProxyResponse[T](
                                       statusCode: Int,
                                       headers: Option[Map[String, String]] = None,
                                       body: Option[T] = None,
                                       isBase64Encoded: Boolean = false
                                     )

object APIGatewayProxyResponse {
  import io.circe.generic.semiauto._
  def encode[T](implicit typeEncoder: Encoder[T]): Encoder[APIGatewayProxyResponse[T]] = deriveEncoder[APIGatewayProxyResponse[T]]
}
