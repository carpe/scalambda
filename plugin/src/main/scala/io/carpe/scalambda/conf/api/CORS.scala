package io.carpe.scalambda.conf.api

sealed trait CORS

object CORS {

  /**
   * This type of CORS handling will automatically add an OPTIONS request handler that will allows browsers to invoke
   * the function.
   *
   * It does this by creating a resource on the ApiGateway instance that returns an response with an
   * `Access-Control-Allow-Origin` header set to `*`.
   */
  case object AllowAll extends CORS

  /**
   * This type of CORS handling tells scalambda that you want to handle CORS yourself.
   */
  case object AllowNone extends CORS
}
