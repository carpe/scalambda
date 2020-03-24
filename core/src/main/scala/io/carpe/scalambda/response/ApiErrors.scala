package io.carpe.scalambda.response

import cats.data.NonEmptyChain
import io.circe.{Encoder, Json}

case class ApiErrors(errors: NonEmptyChain[ApiError])

object ApiErrors {
  def encoder(implicit apiErrorEncoder: Encoder[ApiError]): Encoder[ApiErrors] = Encoder[ApiErrors](a => {
    Json.obj(
      ("errors", Json.fromValues(a.errors.map(apiErrorEncoder.apply).toChain.toList))
    )
  })

  def apply(apiError: ApiError, apiErrors: ApiError*): ApiErrors = {
    val errors = NonEmptyChain(apiError, apiErrors: _*)
    new ApiErrors(errors = errors)
  }
}
