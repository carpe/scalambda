package io.carpe.scalambda.conf.api

import io.carpe.scalambda.conf.function.Method

import scala.language.postfixOps

case class ApiGatewayEndpoint(url: String, method: Method, auth: Auth, cors: CORS)
