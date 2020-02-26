package io.carpe.scalambda.api.index

import io.carpe.scalambda.api.ApiResourceInput
import io.carpe.scalambda.request.APIGatewayProxyRequest

case class IndexRequest(queryParams: Map[String, String])(val original: APIGatewayProxyRequest.WithoutBody) extends ApiResourceInput[None.type]

object IndexRequest {
  def fromProxyRequest(proxyRequest: APIGatewayProxyRequest.WithoutBody): IndexRequest = {
    IndexRequest(proxyRequest.queryStringParameters)(proxyRequest)
  }
}
