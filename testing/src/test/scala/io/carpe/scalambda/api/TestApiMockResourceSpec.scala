package io.carpe.scalambda.api

import io.carpe.scalambda.api.api.TestApi
import io.carpe.scalambda.testing.api.MockApiResourceSpec
import org.scalatest.flatspec.AnyFlatSpec

class TestApiMockResourceSpec extends AnyFlatSpec with MockApiResourceSpec[TestApi] {
  /**
   * You can use this function to inject mocks into your ApiResource handlers.
   *
   * @param api which you can manipulate or omit entirely in favor of your own instance
   * @return the actual instance of api that will be used by your handlers during tests
   */
  override def mockApi(api: TestApi): TestApi = api
}
