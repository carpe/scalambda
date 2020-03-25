package io.carpe.scalambda.conf.function

case class ApiGatewayConf(route: String, method: Method, authConf: AuthConf)