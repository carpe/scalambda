package io.carpe.scalambda.conf.api

import io.carpe.scalambda.conf.function.Method

case class ApiGatewayConfig(route: String, method: Method, authConf: Auth, cors: CORS)