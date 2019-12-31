package io.carpe.scalambda.response

import io.circe.{Encoder, Json}

case class APIGatewayProxyResponse[T](statusCode: Int,
                                      headers: Map[String, String] = Map.empty,
                                      body: Option[T] = None,
                                      isBase64Encoded: Boolean = false
                                     )

object APIGatewayProxyResponse {


  implicit def encode[T](implicit typeEncoder: Encoder[T]): Encoder[APIGatewayProxyResponse[T]] = {
    import io.circe.syntax._

    Encoder.forProduct4("statusCode", "headers", "body", "isBase64Encoded")(response => {
      (response.statusCode, response.headers, response.body.asJson.noSpaces, response.isBase64Encoded)
    })
  }
}
