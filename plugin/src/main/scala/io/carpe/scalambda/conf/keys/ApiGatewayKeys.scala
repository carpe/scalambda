package io.carpe.scalambda.conf.keys

import cats.data.Chain
import io.carpe.scalambda.conf.api.{ApiDomain, ApiGatewayConfig, ApiGatewayEndpoint}
import io.carpe.scalambda.conf.function.{Auth, Method}
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

  lazy val Auth: io.carpe.scalambda.conf.function.Auth.type = io.carpe.scalambda.conf.function.Auth


  /**
   * Old methods for endpoint configuration
   */

  def post(route: String, authConf: Auth = Auth.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.POST, authConf)
  }

  def get(route: String, authConf: Auth = Auth.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.GET, authConf)
  }

  def put(route: String, authConf: Auth = Auth.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.PUT, authConf)
  }

  def delete(route: String, authConf: Auth = Auth.AllowAll): ApiGatewayConfig = {
    api.ApiGatewayConfig(route = route, method = Method.DELETE, authConf)
  }

  /**
   * Helplers for Endpoint Configuration
   */

  def POST(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.POST, authConfig)
  def GET(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.GET, authConfig)
  def PUT(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.PUT, authConfig)
  def PATCH(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.PATCH, authConfig)
  def DELETE(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.DELETE, authConfig)
  def HEAD(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.HEAD, authConfig)
  def OPTIONS(route: String)(implicit authConfig: Auth = Auth.AllowAll): ApiGatewayEndpoint = api.ApiGatewayEndpoint(route, method = Method.OPTIONS, authConfig)



}

