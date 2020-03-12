package io.carpe.scalambda.conf.function

sealed trait AuthConf

object AuthConf {

//  case class Authorized(authorizerName: String) extends AuthConf {
//    lazy val tfVarName: String = s"${StringUtils.toSnakeCase(authorizerName)}_invoke_arn"
//  }

  case object CarpeAuthorizer extends AuthConf

  case object Unauthorized extends AuthConf

//  def authorized(authorizerName: String): Authorized = Authorized(authorizerName)

  lazy val unauthorized: Unauthorized.type = Unauthorized
}
