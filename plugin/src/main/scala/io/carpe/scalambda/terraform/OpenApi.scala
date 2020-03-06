package io.carpe.scalambda.terraform

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.openapi.{ResourceMethod, ResourcePath}

case class OpenApi(paths: List[ResourcePath])

object OpenApi {

  /**
   * Helper for creating [[OpenApi]] from list of functions.
   *
   * @param scalambdaFunctions functions to create api from
   * @return an OpenAPI that wraps the provided functions
   */
  def forFunctions(scalambdaFunctions: List[ScalambdaFunction]): OpenApi = {
    val functionsByRoute: Map[String, List[ScalambdaFunction]] = scalambdaFunctions
      .groupBy(_.apiConfig.map(_.route))
      .flatMap({ case (maybeRoute, functions) => maybeRoute.map(_ -> functions)})

    val resourcePaths = functionsByRoute.map({ case (resourcePath, functions) =>
      // TODO: Optionally add this OPTIONS request. Or at least make the response configurable
      val defaultOptionsMethod = Some(ResourceMethod.optionsMethod)

      functions.foldRight(ResourcePath(resourcePath, post = None, get = None, put = None, delete = None, options = defaultOptionsMethod))((function, resourcePath) => {
        resourcePath.addFunction(function)
      })
    }).toList

    OpenApi(resourcePaths)
  }

  import io.circe._

  lazy implicit val encoder: Encoder[OpenApi] = (api: OpenApi) => Json.obj(
    ("swagger", Json.fromString("2.0")),
    ("info", Json.fromJsonObject(JsonObject.fromMap(
      Map(
        "version" -> Json.fromString("latest"),
        "title" -> Json.fromString("${api_name}")
      )
    ))),
    ("schemes", Json.fromValues(
      List(
        Json.fromString("https")
      )
    )),
    ("paths", Json.obj(
      api.paths.map(path => {
        path.name -> ResourcePath.encoder.apply(path)
      }): _*
    ))
  )

  lazy val printer: yaml.Printer =  io.circe.yaml.Printer(preserveOrder = true, dropNullKeys = true)

  def apiToYaml(api: OpenApi): String = {
    val encodedApi = encoder.apply(api)
    printer.pretty(encodedApi)
  }
}