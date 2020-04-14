package io.carpe.scalambda.conf.function

case class ApiGatewayConfig(route: String, method: Method, authConf: AuthConfig)