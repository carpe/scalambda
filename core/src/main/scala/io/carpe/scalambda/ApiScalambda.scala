package io.carpe.scalambda

import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.circe.{Decoder, Encoder}

abstract class ApiScalambda[I, O](implicit val rDecode: Decoder[I], val rEncode: Encoder[O])
  extends Scalambda[APIGatewayProxyRequest.WithBody[I], APIGatewayProxyResponse[O]]
