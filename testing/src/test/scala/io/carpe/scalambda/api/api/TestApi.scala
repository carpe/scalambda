package io.carpe.scalambda.api.api

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.conf.{ApiBootstrap, ScalambdaApi}

case class TestApi(lambdaContext: Context) extends ScalambdaApi(lambdaContext)

object TestApi {
  implicit val testApiBootstrap: ApiBootstrap[TestApi] = new ApiBootstrap[TestApi] {
    override def apply(context: Context): TestApi = TestApi(context)
  }
}
