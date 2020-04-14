package io.carpe.scalambda.terraform.openapi

import io.circe.{Encoder, Json}

sealed trait SecurityDefinition {
  def authorizerName: String
}

object SecurityDefinition {

  case class Authorizer(authorizerName: String, authorizerArn: String, authorizerRole: String) extends SecurityDefinition

  case object ApiKey extends SecurityDefinition {
    override def authorizerName: String = "api_key"
  }


  lazy implicit val encoder: Encoder[SecurityDefinition] = {
    case securityDefinition@Authorizer(authorizerName, authorizerArn, authorizerRole) =>
      Json.obj(
        ("type" -> Json.fromString("apiKey")),
        ("name" -> Json.fromString("Authorization")),
        "in" -> Json.fromString("header"),
        "x-amazon-apigateway-authtype" -> Json.fromString("custom"),
        "x-amazon-apigateway-authorizer" -> Json.obj(
          "authorizerUri" -> Json.fromString(authorizerArn),
          "authorizerCredentials" -> Json.fromString(authorizerRole),
          "authorizerResultTtlInSeconds" -> Json.fromInt(300),
          "identityValidationExpression" -> Json.fromString("^Bearer [-0-9a-zA-z\\.]*$"),
          "type" -> Json.fromString("token")
        )
      )
    case ApiKey =>
      Json.obj(
        "type" -> Json.fromString("apiKey"),
        "name" -> Json.fromString("x-api-key"),
        "in" -> Json.fromString("header")
      )
  }
}