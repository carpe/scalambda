package io.carpe.scalambda.terraform

import io.carpe.scalambda.ScalambdaFunction
import io.carpe.scalambda.terraform.openapi.{Info, ResourcePath}

case class OpenApi(paths: List[ResourcePath])

object OpenApi {

  /**
   * Helper for creating [[OpenApi]] from list of functions.
   *
   * @param scalambdaFunction functions to create api from
   * @return an OpenAPI that wraps the provided functions
   */
  def forFunctions(scalambdaFunction: List[ScalambdaFunction]): OpenApi = {
    OpenApi(
      List.empty
    )
  }

  import io.circe._
  import io.circe.yaml.syntax._

  lazy implicit val encoder: Encoder[OpenApi] = new Encoder[OpenApi] {
    final def apply(openApi: OpenApi): Json = Json.obj(
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
      ))
    )
  }

  def apiToYaml(api: OpenApi): String = {
    val encodedApi = encoder.apply(api)
    encodedApi.asYaml.spaces2

  }
}
