package io.carpe.scalambda.terraform.openapi.resourcemethod

import io.circe.{Encoder, Json}

case class MethodResponse(statusCode: Int, description: String, headers: List[(String, String)])

object MethodResponse {
  implicit val encoder: Encoder[MethodResponse] = (response: MethodResponse) => {
    val responseFields = List(
      {
        Some("description" -> Json.fromString(response.description))
      },
      {
        if (response.headers.nonEmpty) {
          Some("headers" -> Json.obj(
            response.headers.map({ case (header, headerType) =>
              header -> Json.obj(
                "type" -> Json.fromString(headerType)
              )
            }): _*
          ))
        } else {
          None
        }
      }
    ).flatten

    Json.obj(responseFields: _*)
  }
}
