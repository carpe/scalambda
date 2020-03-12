package io.carpe.scalambda.conf.function

case class ApiGatewayConf(route: String, method: Method, authConf: AuthConf)

object ApiGatewayConf {
  def post(route: String, authConf: AuthConf = AuthConf.CarpeAuthorizer): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.POST, authConf)
  }

  def get(route: String, authConf: AuthConf = AuthConf.CarpeAuthorizer): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.GET, authConf)
  }

  def put(route: String, authConf: AuthConf = AuthConf.CarpeAuthorizer): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.PUT, authConf)
  }

  def delete(route: String, authConf: AuthConf = AuthConf.CarpeAuthorizer): ApiGatewayConf = {
    ApiGatewayConf(route = route, method = Method.DELETE, authConf)
  }

}
