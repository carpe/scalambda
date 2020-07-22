package io.carpe.scalambda.conf.function

import io.carpe.scalambda.terraform.openapi.SecurityDefinition

sealed trait Auth {
  def securityDefinitions: Seq[SecurityDefinition]
}

object Auth {

  /**
   * A Custom "Token Authorizer" that you can use to implement your own auth logic for ApiGateway.
   *
   * It requires users to provide an Authorization header that will include a Bearer token.
   *
   * This authorizer will invoke the desired lambda function as a TOKEN authorizer. See the docs below for more information:
   * https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-lambda-authorizer-input.html
   *
   * @param tfVariableName for the generated terraform variables that will allow you to connect your ApiGateway to your authorizer. Can be any snake_case string you'd like.
   */
  case class TokenAuthorizer(tfVariableName: String) extends Auth {
    override def securityDefinitions: Seq[SecurityDefinition] = Seq(SecurityDefinition.TokenAuthorizer(
      authorizerName = tfVariableName
    ))
  }

  /**
   * A Custom Authorizer that you can use to implement your own auth logic for ApiGateway.
   *
   * This authorizer will invoke the desired lambda function as a TOKEN authorizer. See the docs below for more information:
   * https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-lambda-authorizer-input.html
   *
   * @param tfVariableName for the generated terraform variables that will allow you to connect your ApiGateway to your authorizer. Can be any snake_case string you'd like.
   */
  case class RequestAuthorizer(tfVariableName: String, identitySources: Seq[String]) extends Auth {
    override def securityDefinitions: Seq[SecurityDefinition] = Seq(SecurityDefinition.RequestAuthorizer(
      authorizerName = tfVariableName, identitySources = identitySources
    ))
  }

  /**
   * Combine multiple Authorizers together.
   *
   * This method of Auth is useful if you would like to both inject metadata via an authorizer, as well as take
   * advantage of Api Gateway Usage Plans, which allows you enforce things like rate limiting for your users.
   *
   * @param authorizers the authorizers you would like to use
   */
  case class Multiple(authorizers: Auth*) extends Auth {
    override def securityDefinitions: Seq[SecurityDefinition] = authorizers.flatMap(_.securityDefinitions)
  }

  /**
   * Require an ApiKey to be passed via the "X-Api-Key" header
   */
  case object ApiKey extends Auth {
    override def securityDefinitions: Seq[SecurityDefinition] = Seq(SecurityDefinition.ApiKey)
  }

  case object AllowAll extends Auth {
    override def securityDefinitions: Seq[SecurityDefinition] = Nil
  }

  /**
   * Sets an endpoint to require no authorization.
   */
  lazy val unauthorized: AllowAll.type = AllowAll
}
