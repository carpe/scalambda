package io.carpe.scalambda.api.index

import io.carpe.scalambda.api.ApiResourceInput
import io.carpe.scalambda.request.APIGatewayProxyRequest

case class IndexRequest(queryParams: Map[String, String])(val original: APIGatewayProxyRequest[None.type]) extends ApiResourceInput[None.type]

object IndexRequest {
  def fromProxyRequest(proxyRequest: APIGatewayProxyRequest[None.type]): IndexRequest = {
    IndexRequest(proxyRequest.queryStringParameters)(proxyRequest)
  }
}
