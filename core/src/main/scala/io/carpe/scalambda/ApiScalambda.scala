package io.carpe.scalambda

import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.circe.{Decoder, Encoder}

abstract class ApiScalambda[R](implicit val rEncode: Encoder[R], val rDecode: Decoder[R])
  extends Scalambda[APIGatewayProxyRequest[R], APIGatewayProxyResponse[R]]
