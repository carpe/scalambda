package io.carpe.scalambda.conf.function

case class ApiGatewayConf(route: String, method: Method)

object ApiGatewayConf {
  def post(route: String): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.POST)
  }

  def get(route: String): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.GET)
  }

  def put(route: String): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.PUT)
  }

  def delete(route: String): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.DELETE)
  }

}
