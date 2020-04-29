package io.carpe.scalambda.conf.function

import io.carpe.scalambda.terraform.openapi.SecurityDefinition
import io.carpe.scalambda.terraform.openapi.SecurityDefinition.Authorizer
import io.carpe.scalambda.terraform.openapi.resourcemethod.Security

sealed trait Auth {
  def authorizer: Option[SecurityDefinition]
}

object Auth {

  /**
   * A Custom Authorizer that you can use to implement your own auth logic for ApiGateway
   * @param authorizerName name for your authorizer. Can be any snake_case string you'd like. Will show up on swagger.yaml
   * @param authorizerArn invoke arn for your authorizer (example: s"arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:1234567889:function:MyAuthorizer/invocations")
   * @param authorizerRole role to assume to allow invocation of your authorizer
   */
  case class Authorizer(authorizerName: String, authorizerArn: String, authorizerRole: String) extends Auth {
    override def authorizer: Option[SecurityDefinition] = Some(SecurityDefinition.Authorizer(
      authorizerName = authorizerName,
      authorizerArn = authorizerArn,
      authorizerRole = authorizerRole
    ))
  }

  case object ApiKey extends Auth {
    override def authorizer: Option[SecurityDefinition] = Some(SecurityDefinition.ApiKey)
  }

  case object AllowAll extends Auth {
    override def authorizer: Option[SecurityDefinition] = None
  }

  /**
   * Sets an endpoint to require no authorization.
   */
  lazy val unauthorized: AllowAll.type = AllowAll
}
