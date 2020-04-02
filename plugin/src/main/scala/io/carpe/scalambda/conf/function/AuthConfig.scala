package io.carpe.scalambda.conf.function

sealed trait AuthConfig

object AuthConfig {

//  case class Authorized(authorizerName: String) extends AuthConf {
//    lazy val tfVarName: String = s"${StringUtils.toSnakeCase(authorizerName)}_invoke_arn"
//  }

  case object CarpeAuthorizer extends AuthConfig

  case object Unauthorized extends AuthConfig

//  def authorized(authorizerName: String): Authorized = Authorized(authorizerName)

  lazy val unauthorized: Unauthorized.type = Unauthorized
}
