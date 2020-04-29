package io.carpe.scalambda.terraform

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiGatewayConfig
import io.carpe.scalambda.terraform.openapi.{ResourceMethod, ResourcePath, SecurityDefinition}

case class OpenApi(paths: Seq[ResourcePath], securityDefinitions: Seq[SecurityDefinition])

object OpenApi {

  /**
   * Helper for creating [[OpenApi]] from list of functions.
   *
   * @param scalambdaFunctions functions to create api from
   * @return an OpenAPI that wraps the provided functions
   */
  def forFunctions(scalambdaFunctions: Seq[ScalambdaFunction]): OpenApi = {
    val functionsByRoute: Map[String, Seq[(ApiGatewayConfig, ScalambdaFunction)]] = scalambdaFunctions
        .flatMap(lambda => lambda match {
          case ScalambdaFunction.Function(naming, handlerPath, functionSource, iamRole, functionConfig, vpcConfig, provisionedConcurrency, environmentVariables) =>
            None
          case ScalambdaFunction.ApiFunction(naming, handlerPath, functionSource, iamRole, functionConfig, vpcConfig, provisionedConcurrency, apiConfig, environmentVariables) =>
            Some(apiConfig -> lambda)
          case ScalambdaFunction.ReferencedFunction(_, _, apiConfig) =>
            Some(apiConfig -> lambda)
        })
      .groupBy(_._1.route)
      .map({ case (route, functions) => route -> functions })

    val resourcePaths = functionsByRoute.map({ case (resourcePath, functions) =>
      // TODO: Optionally add this OPTIONS request. Or at least make the response configurable
      val defaultOptionsMethod = Some(ResourceMethod.optionsMethod)

      functions.foldRight(ResourcePath(resourcePath, post = None, get = None, put = None, delete = None, options = defaultOptionsMethod))((function, resourcePath) => {
        resourcePath.addFunction(function._1, function._2)
      })
    }).toList

    val securityDefinitions = functionsByRoute.flatMap(_._2.flatMap(_._1.authConf.authorizer)).toSeq

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

  lazy val printer: yaml.Printer =  io.circe.yaml.Printer(preserveOrder = true, dropNullKeys = true)

  def apiToYaml(api: OpenApi): String = {
    val encodedApi = encoder.apply(api)
    printer.pretty(encodedApi)
  }
}
