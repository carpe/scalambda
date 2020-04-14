package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.{ApiGatewayConfig, AuthConfig, Method}

trait ApiGatewayKeys {
  def post(route: String, authConf: AuthConfig = AuthConfig.AllowAll): ApiGatewayConfig = {
    ApiGatewayConfig(route = route, method = Method.POST, authConf)
  }

  def get(route: String, authConf: AuthConfig = AuthConfig.AllowAll): ApiGatewayConfig = {
    ApiGatewayConfig(route = route, method = Method.GET, authConf)
  }

  def put(route: String, authConf: AuthConfig = AuthConfig.AllowAll): ApiGatewayConfig = {
    ApiGatewayConfig(route = route, method = Method.PUT, authConf)
  }

  def delete(route: String, authConf: AuthConfig = AuthConfig.AllowAll): ApiGatewayConfig = {
    ApiGatewayConfig(route = route, method = Method.DELETE, authConf)
  }

}

