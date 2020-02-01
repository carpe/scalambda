package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging

abstract class ScalambdaApi(lambdaContext: Context) extends LazyLogging

object ScalambdaApi {
  case class Default(lambdaContext: Context) extends ScalambdaApi(lambdaContext)
}