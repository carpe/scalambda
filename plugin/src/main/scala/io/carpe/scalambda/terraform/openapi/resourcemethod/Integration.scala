package io.carpe.scalambda.terraform.openapi.resourcemethod

import io.carpe.scalambda.conf.ScalambdaFunction
import io.circe.{Encoder, Json}

sealed trait Integration

object Integration {

  case class LambdaIntegration(function: ScalambdaFunction) extends Integration

  case class AllowOrigin(origin: String) extends Integration

  implicit val encoder: Encoder[Integration] = {
    case LambdaIntegration(function) =>
      Json.obj(
        "uri" -> Json.fromString("""${""" + function.swaggerVariableName + """}"""),
        "passthroughBehavior" -> Json.fromString("when_no_match"),
        "httpMethod" -> Json.fromString("POST"),
        "type" -> Json.fromString("aws_proxy")
      )
    case AllowOrigin(origin) =>
      Json.obj(
        "type" -> Json.fromString("mock"),
        "requestTemplates" -> Json.obj(
          "application/json" -> Json.fromString("{\"statusCode\":200}")
        ),
        "responses" -> Json.obj(
          "default" -> Json.obj(
            "statusCode" -> Json.fromString("200"),
            "responseParameters" -> Json.obj(
              "method.response.header.Access-Control-Allow-Headers" -> Json.fromString("'Content-Type,X-Amz-Date,Authorization,X-Api-Key'"),
              "method.response.header.Access-Control-Allow-Methods" -> Json.fromString("'*'"),
              "method.response.header.Access-Control-Allow-Origin" -> Json.fromString(s"'${origin}'"),
              "method.response.header.Access-Control-Max-Age" -> Json.fromString("'600'")
            ),
            "responseTemplates" -> Json.obj(
              "application/json" -> Json.fromString("{}")
            )
          )
        )
      )
  }
}