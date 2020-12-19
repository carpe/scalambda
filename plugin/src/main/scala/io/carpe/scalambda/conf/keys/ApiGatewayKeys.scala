package io.carpe.scalambda.conf.keys

import cats.data.Chain
import io.carpe.scalambda.conf.api.{ApiDomain, ApiGatewayConfig, ApiGatewayEndpoint, Auth, CORS}
import io.carpe.scalambda.conf.function.Method
import io.carpe.scalambda.conf.{ScalambdaFunction, api}
import sbt.settingKey

trait ApiGatewayKeys {

  /**
   * Api related settings
   */

  lazy val domainName = settingKey[ApiDomain]("Domain name to be used in the terraform output")

  lazy val apiName = settingKey[String]("Prefix for the name of the api. Defaults to project name")

  lazy val scalambdaApiEndpoints = settingKey[Chain[(ApiGatewayEndpoint, ScalambdaFunction)]]("Mapping of all the ApiGateway endpoints to the Lambda Functions that they invoke.")

  /**
   * Helpers for domain settings
   */

  lazy val ApiDomain: io.carpe.scalambda.conf.api.ApiDomain.type = io.carpe.scalambda.conf.api.ApiDomain

  /**
   * Helpers for auth
   */

  lazy val Auth: io.carpe.scalambda.conf.api.Auth.type = io.carpe.scalambda.conf.api.Auth

  lazy val CORS: CORS.type = CORS


  /**
   * Old methods for endpoint configuration
   */

  def post(route: String, authConf: Auth = Auth.AllowAll, cors: CORS = CORS.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.POST, authConf, cors)
  }

  def get(route: String, authConf: Auth = Auth.AllowAll, cors: CORS = CORS.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.GET, authConf, cors)
  }

  def put(route: String, authConf: Auth = Auth.AllowAll, cors: CORS = CORS.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.PUT, authConf, cors)
  }

  def delete(route: String, authConf: Auth = Auth.AllowAll, cors: CORS = CORS.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.DELETE, authConf, cors)
  }

  /**
   * Helpers for Endpoint Configuration
   */

  def POST(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.POST, authConfig, cors)
  def GET(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.GET, authConfig, cors)
  def PUT(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.PUT, authConfig, cors)
  def PATCH(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.PATCH, authConfig, cors)
  def DELETE(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.DELETE, authConfig, cors)
  def HEAD(route: String, cors: CORS = CORS.AllowAll)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.HEAD, authConfig, cors)
  def OPTIONS(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.OPTIONS, authConfig, cors = CORS.AllowNone)

}

