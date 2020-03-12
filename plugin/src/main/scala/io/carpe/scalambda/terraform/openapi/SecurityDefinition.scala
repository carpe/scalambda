package io.carpe.scalambda.terraform.openapi

import io.circe.{Encoder, Json}

case class SecurityDefinition(authorizerName: String, authorizerArn: String, authorizerRole: String)

object SecurityDefinition {
  lazy implicit val encoder: Encoder[SecurityDefinition] = (securityDefinition: SecurityDefinition) => Json.obj(
    ("type" -> Json.fromString("apiKey")),
    ("name" -> Json.fromString("Authorization")),
    "in" -> Json.fromString("header"),
    "x-amazon-apigateway-authtype" -> Json.fromString("custom"),
    "x-amazon-apigateway-authorizer" -> Json.obj(
      "authorizerUri" -> Json.fromString(securityDefinition.authorizerArn),
      "authorizerCredentials" -> Json.fromString(securityDefinition.authorizerRole),
      "authorizerResultTtlInSeconds" -> Json.fromInt(300),
      "identityValidationExpression" -> Json.fromString("^Bearer [-0-9a-zA-z\\.]*$"),
      "type" -> Json.fromString("token")
    )
  )
}