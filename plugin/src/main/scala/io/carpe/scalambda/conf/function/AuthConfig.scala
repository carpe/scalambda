package io.carpe.scalambda.conf.function

import io.carpe.scalambda.terraform.openapi.SecurityDefinition
import io.carpe.scalambda.terraform.openapi.SecurityDefinition.Authorizer
import io.carpe.scalambda.terraform.openapi.resourcemethod.Security

sealed trait AuthConfig {
  def authorizer: Option[SecurityDefinition]
}

object AuthConfig {

//  case class Authorized(authorizerName: String) extends AuthConf {
//    lazy val tfVarName: String = s"${StringUtils.toSnakeCase(authorizerName)}_invoke_arn"
//  }

  case object CarpeAuthorizer extends AuthConfig {
    override def authorizer: Option[SecurityDefinition] = Some(Authorizer(
      authorizerName = Security.carpeAuthorizer.name,
      authorizerArn = s"arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:120864075170:function:CarpeAuthorizerProd/invocations",
      authorizerRole = "arn:aws:iam::120864075170:role/Auth0Integration"
    ))
  }

  case object ApiKey extends AuthConfig {
    override def authorizer: Option[SecurityDefinition] = Some(SecurityDefinition.ApiKey)
  }

  case object Unauthorized extends AuthConfig {
    override def authorizer: Option[SecurityDefinition] = None
  }

//  def authorized(authorizerName: String): Authorized = Authorized(authorizerName)

  lazy val unauthorized: Unauthorized.type = Unauthorized
}
