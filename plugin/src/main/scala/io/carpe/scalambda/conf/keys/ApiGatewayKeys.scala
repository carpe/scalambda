package io.carpe.scalambda.conf.keys

import sbt.settingKey
import io.carpe.scalambda.conf.function.{ApiGatewayConfig, AuthConfig, Method}

trait ApiGatewayKeys {

  /**
   * Api related settings
   */

  lazy val apiName = settingKey[String]("Prefix for the name of the api. Defaults to project name")
  lazy val apiAuthorizerArn = settingKey[String]("Arn for custom authorizer to use for ApiGateway")

  /**
   * Helpers for configuration
   */

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

