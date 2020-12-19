package io.carpe.scalambda.terraform

import cats.Eval
import cats.data.NonEmptyList
import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.{ApiGatewayEndpoint, CORS}
import io.carpe.scalambda.terraform.openapi.{ResourceMethod, ResourcePath, SecurityDefinition}
import org.apache.http.client.methods.RequestBuilder.options

case class OpenApi(paths: Seq[ResourcePath], securityDefinitions: Seq[SecurityDefinition])

object OpenApi {

  // a default OPTIONS request handler that gives browsers the permission needed to make special requests;
  // this is needed by almost all API requests thanks to some arguably bad decisions made many years ago.
  private val defaultOptionsMethod: ResourceMethod = ResourceMethod.optionsMethod

  /**
   * Helper for creating [[OpenApi]] from list of functions.
   *
   * @param endpointMappings endpoints mapped to the lambda functions that they invoke, essentially a definition of the api
   * @return an OpenAPI that wraps the provided functions
   */
  def forFunctions(endpointMappings: NonEmptyList[(ApiGatewayEndpoint, ScalambdaFunction)]): OpenApi = {
    import cats.implicits._

    val functionsByRoute: Map[String, NonEmptyList[(ApiGatewayEndpoint, ScalambdaFunction)]] = endpointMappings.groupBy({ case (endpoint, lambda) =>
      endpoint.url
    })

    val resourcePaths: List[ResourcePath] = functionsByRoute.map({ case (route, functions) =>
      functions.foldRight(Eval.now(ResourcePath.empty(route)))((endpointMapping, resourcePath) => {
        resourcePath
          // add the function to the resource
          .map(_.addFunction(endpointMapping._1, endpointMapping._2))
          // provide a default OPTIONS method handler, if one has not yet been defined and the function is set to AllowAll
          .map(resource => {
            if (resource.options.isEmpty && endpointMapping._1.cors == CORS.AllowAll) {
              resource.copy(options = Some(defaultOptionsMethod))
            } else {
              resource
            }
          })
      }).value
    }).toList

    // get all the unique security definitions for all the functions
    val securityDefinitions = endpointMappings.toList.flatMap({ case (endpoint, _) => endpoint.auth.securityDefinitions }).distinct

    OpenApi(resourcePaths, securityDefinitions)
  }

  import io.circe._

  lazy implicit val encoder: Encoder[OpenApi] = (api: OpenApi) => Json.obj(
    "swagger" -> Json.fromString("2.0"),
    "info" -> Json.fromJsonObject(JsonObject.fromMap(
      Map(
        "version" -> Json.fromString("latest"),
        "title" -> Json.fromString("${api_name}")
      )
    )),
    "schemes" -> Json.fromValues(
      List(
        Json.fromString("https")
      )
    ),
    "paths" -> Json.obj(
      api.paths.map(path => {
        path.name -> ResourcePath.encoder.apply(path)
      }): _*
    ),
    "securityDefinitions" -> Json.obj(
      api.securityDefinitions.map(securityDefinition => {
        securityDefinition.authorizerName -> SecurityDefinition.encoder.apply(securityDefinition)
      }): _*
    )
  )

  lazy val printer: yaml.Printer = io.circe.yaml.Printer(preserveOrder = true, dropNullKeys = true)

  def apiToYaml(api: OpenApi): String = {
    val encodedApi = encoder.apply(api)
    printer.pretty(encodedApi)
  }
}
