package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context

abstract class ScalambdaApi(lambdaContext: Context)

object ScalambdaApi {
  case class Default(lambdaContext: Context) extends ScalambdaApi(lambdaContext)
}