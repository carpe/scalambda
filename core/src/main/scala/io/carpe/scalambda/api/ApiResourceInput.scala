package io.carpe.scalambda.api

import io.carpe.scalambda.request.APIGatewayProxyRequest

trait ApiResourceInput[T] {
  def original: APIGatewayProxyRequest[T]
}
