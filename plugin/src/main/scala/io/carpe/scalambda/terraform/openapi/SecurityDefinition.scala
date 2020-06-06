package io.carpe.scalambda.terraform.openapi


import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.circe.{Encoder, Json}

sealed trait SecurityDefinition {
  def authorizerName: String
}

object SecurityDefinition {

  case class TokenAuthorizer(authorizerName: String) extends SecurityDefinition {
    lazy val formattedName: String = StringUtils.toSnakeCase(authorizerName)
    lazy val authorizerUriVariable: String = s"${authorizerName}_uri"
    lazy val authorizerRoleVariable: String = s"${authorizerName}_role"

    lazy val variables = Seq(
      Variable(name = authorizerUriVariable, description = Some(s"Invoke Uri for the ${authorizerName} Authorizer. Example: arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:123456790:function:MyAuthorizer/invocations"), None),
      Variable(name = authorizerRoleVariable, description = Some(s"IAM Role for the ${authorizerName} Authorizer"), None)
    )
  }

  case class RequestAuthorizer(authorizerName: String, identitySources: Seq[String]) extends SecurityDefinition {
    lazy val formattedName: String = StringUtils.toSnakeCase(authorizerName)
    lazy val authorizerUriVariable: String = s"${authorizerName}_uri"
    lazy val authorizerRoleVariable: String = s"${authorizerName}_role"

    lazy val variables = Seq(
      Variable(name = authorizerUriVariable, description = Some(s"Invoke Uri for the ${authorizerName} Authorizer. Example: arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:123456790:function:MyAuthorizer/invocations"), None),
      Variable(name = authorizerRoleVariable, description = Some(s"IAM Role for the ${authorizerName} Authorizer"), None)
    )
  }

  case object ApiKey extends SecurityDefinition {
    override def authorizerName: String = "api_key"
  }

  lazy implicit val encoder: Encoder[SecurityDefinition] = {
    case securityDefinition: TokenAuthorizer =>
      Json.obj(
        "type" -> Json.fromString("apiKey"),
        "name" -> Json.fromString("Authorization"),
        "in" -> Json.fromString("header"),
        "x-amazon-apigateway-authtype" -> Json.fromString("custom"),
        "x-amazon-apigateway-authorizer" -> Json.obj(
          "authorizerUri" -> Json.fromString("${" + securityDefinition.authorizerUriVariable + "}"),
          "authorizerCredentials" -> Json.fromString("${" + securityDefinition.authorizerRoleVariable + "}"),
          "authorizerResultTtlInSeconds" -> Json.fromInt(300),
          "identityValidationExpression" -> Json.fromString("^Bearer [-0-9a-zA-z\\.]*$"),
          "type" -> Json.fromString("token")
        )
      )
    case requestAuthorizer: RequestAuthorizer => {
      Json.obj(
        "type" -> Json.fromString("apiKey"),
        "name" -> Json.fromString("Unused"),
        "in" -> Json.fromString("header"),
        "x-amazon-apigateway-authtype" -> Json.fromString("custom"),
        "x-amazon-apigateway-authorizer" -> Json.obj(
          "type" -> Json.fromString("request"),
          "identitySource" -> Json.fromString(requestAuthorizer.identitySources.mkString(",")),
          "authorizerCredentials" -> Json.fromString("${" + requestAuthorizer.authorizerRoleVariable + "}"),
          "authorizerUri" -> Json.fromString("${" + requestAuthorizer.authorizerUriVariable + "}"),
          "authorizerResultTtlInSeconds" -> Json.fromInt(300)
        )
      )
    }

    case ApiKey =>
      Json.obj(
        "type" -> Json.fromString("apiKey"),
        "name" -> Json.fromString("x-api-key"),
        "in" -> Json.fromString("header")
      )
  }
}