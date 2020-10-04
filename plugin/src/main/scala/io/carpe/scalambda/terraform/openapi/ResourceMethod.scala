package io.carpe.scalambda.terraform.openapi

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiGatewayEndpoint
import io.carpe.scalambda.terraform.openapi.resourcemethod.Integration.{AllowOrigin, LambdaIntegration}
import io.carpe.scalambda.terraform.openapi.resourcemethod.{Integration, MethodResponse, Security}
import io.circe.{Encoder, Json}

case class ResourceMethod(tags: List[String], description: String, consumes: List[String], security: List[Security],
                          responses: List[MethodResponse], integration: Integration
                         )

object ResourceMethod {

  lazy val optionsMethod =
    new ResourceMethod(
      tags = List("Meta"), description = "Used to handle CORS on UI", consumes = List("application/json"),
      security = List.empty,
      responses = List(
        MethodResponse(200, description = "Default response for CORS method", headers = List(
          "Access-Control-Allow-Headers" -> "string",
          "Access-Control-Allow-Methods" -> "string",
          "Access-Control-Allow-Origin" -> "string",
          "Access-Control-Max-Age" -> "integer"
        ))
      ),
      integration = AllowOrigin("*")
    )

  def fromLambda(endointMapping: ApiGatewayEndpoint, lambda: ScalambdaFunction): ResourceMethod = {
    new ResourceMethod(
      List.empty,
      description = "TBD",
      consumes = List("application/json"),
      security = {
        // get all the unique security definitions for the endpoint
        val securityDefinitions = endointMapping.auth.securityDefinitions
        // convert security definitions to security elements to add to the swagger definition
        securityDefinitions.map(Security(_)).toList
      },
      responses = List(
        MethodResponse(200, "Request completed without errors!", List.empty)
      ),
      integration = LambdaIntegration(lambda)
    )
  }

  implicit val encoder: Encoder[ResourceMethod] = (method: ResourceMethod) => Json.obj(
    "tags" -> Json.fromValues(method.tags.map(Json.fromString)),
    "description" -> Json.fromString(method.description),
    "consumes" -> Json.fromValues(method.consumes.map(Json.fromString)),
    "security" -> Json.fromValues(method.security.map(Security.encoder.apply)),
    "responses" -> Json.obj(
      method.responses.map(response => {
        response.statusCode.toString -> MethodResponse.encoder.apply(response)
      }): _*
    ),
    "x-amazon-apigateway-integration" -> Integration.encoder.apply(method.integration)
  )
}