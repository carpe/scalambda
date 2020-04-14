package io.carpe.scalambda.api.testapi

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.conf.{ApiBootstrap, ScalambdaApi}
import io.carpe.scalambda.fixtures.TestModels.Car

case class TestApi(lambdaContext: Context) extends ScalambdaApi {
  def queryDatabase(hp: Int): Option[Car] = {
    // throw an exception here because we want to make sure that test helpers can be used to mock calls like these.
    throw new RuntimeException("There was an unmocked call to the fake database inside TestApi.")
  }
}

object TestApi {
  implicit val testApiBootstrap: ApiBootstrap[TestApi] = new ApiBootstrap[TestApi] {
    override def apply(context: Context): TestApi = TestApi(context)
  }
}
