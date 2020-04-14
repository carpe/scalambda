package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.response.{ApiError, ApiErrors}
import io.circe.Encoder

trait ApiBootstrap[C <: ScalambdaApi] {

  /**
   * This encoder will be used to encode each individual error. It is consumed by the errorsEncoder
   *
   * It exists on the ApiBootstrap itself because errors may occur during initialization of the lambda.
   */
  val errorEncoder: Encoder[ApiError] = ApiError.defaultEncoder

  /**
   * This encoder will be used to encode errors.
   *
   * It exists on the ApiBootstrap itself because errors may occur during initialization of the lambda.
   */
  val errorsEncoder: Encoder[ApiErrors] = ApiErrors.encoder(errorEncoder)

  def apply(context: Context): C
}

object ApiBootstrap {
  def apply[C <: ScalambdaApi](a: Context => C): Context => C = a.apply
}
