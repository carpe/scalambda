package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.response.ApiError
import io.circe.Encoder

trait ScalambdaApi extends LazyLogging {
  def lambdaContext: Context
}

object ScalambdaApi {
  case class Default(lambdaContext: Context) extends ScalambdaApi
}