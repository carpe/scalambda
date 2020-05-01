package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.api
import io.carpe.scalambda.conf.api.{ApiDomain, ApiGatewayConfig}
import sbt.settingKey
import io.carpe.scalambda.conf.function.{Auth, Method}

trait ApiGatewayKeys {

  /**
   * Api related settings
   */

  lazy val domainName = settingKey[ApiDomain]("Domain name to be used in the terraform output")

  lazy val apiName = settingKey[String]("Prefix for the name of the api. Defaults to project name")

  /**
   * Helpers for domain settings
   */

  lazy val ApiDomain: io.carpe.scalambda.conf.api.ApiDomain.type = io.carpe.scalambda.conf.api.ApiDomain

  /**
   * Helpers for auth
   */

  lazy val Auth: io.carpe.scalambda.conf.function.Auth.type = io.carpe.scalambda.conf.function.Auth


  /**
   * Helpers for endpoint configuration
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

}

