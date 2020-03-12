package io.carpe.scalambda.terraform

import io.carpe.scalambda.conf.function.AuthConf
import io.carpe.scalambda.conf.function.AuthConf.CarpeAuthorizer
import io.carpe.scalambda.terraform.ast.resources.LambdaFunction
import io.carpe.scalambda.terraform.openapi.resourcemethod.Security
import io.carpe.scalambda.terraform.openapi.{ResourceMethod, ResourcePath, SecurityDefinition}

case class OpenApi(paths: Seq[ResourcePath], securityDefinitions: Seq[SecurityDefinition])

object OpenApi {

  /**
   * Helper for creating [[OpenApi]] from list of functions.
   *
   * @param scalambdaFunctions functions to create api from
   * @return an OpenAPI that wraps the provided functions
   */
  def forFunctions(scalambdaFunctions: Seq[LambdaFunction]): OpenApi = {
    val functionsByRoute: Map[String, Seq[LambdaFunction]] = scalambdaFunctions
      .groupBy(_.scalambdaFunction.apiConfig.map(_.route))
      .flatMap({ case (maybeRoute, functions) => maybeRoute.map(_ -> functions)})

    val resourcePaths = functionsByRoute.map({ case (resourcePath, functions) =>
      // TODO: Optionally add this OPTIONS request. Or at least make the response configurable
      val defaultOptionsMethod = Some(ResourceMethod.optionsMethod)

      functions.foldRight(ResourcePath(resourcePath, post = None, get = None, put = None, delete = None, options = defaultOptionsMethod))((function, resourcePath) => {
        resourcePath.addFunction(function)
      })
    }).toList

    val securityDefinitions = scalambdaFunctions.flatMap(_.scalambdaFunction.apiConfig).map(_.authConf).distinct.flatMap(authConf => {
      authConf match {
        case CarpeAuthorizer =>
          Some(SecurityDefinition(
            authorizerName = Security.carpeAuthorizer.name,
            authorizerArn = "arn:aws:lambda:us-west-2:120864075170:function:CarpeAuthorizer:prod",
            authorizerRole = "arn:aws:iam::120864075170:role/Auth0Integration"
          ))
        case AuthConf.Unauthorized =>
          None
      }
    })

    OpenApi(resourcePaths, securityDefinitions)
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
    )),
    ("securityDefinitions", Json.obj(
      api.securityDefinitions.map(securityDefinition => {
        securityDefinition.authorizerName -> SecurityDefinition.encoder.apply(securityDefinition)
      }): _*
    ))
  )

  lazy val printer: yaml.Printer =  io.circe.yaml.Printer(preserveOrder = true, dropNullKeys = true)

  def apiToYaml(api: OpenApi): String = {
    val encodedApi = encoder.apply(api)
    printer.pretty(encodedApi)
  }
}
