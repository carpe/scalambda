package io.carpe.scalambda.api

import io.carpe.scalambda.api.testapi.{TestApi, TestShow}
import io.carpe.scalambda.testing.api.MockApiResourceSpec
import io.carpe.scalambda.testing.api.behaviors.ApiResourceBehaviors.ShowTestCase
import org.scalatest.flatspec.AnyFlatSpec

class TestApiMockResourceSpec extends AnyFlatSpec with MockApiResourceSpec[TestApi] {

  import io.carpe.scalambda.fixtures.TestModels._


  /**
   * You can use this function to inject mocks into your ApiResource handlers.
   *
   * @param api which you can manipulate or omit entirely in favor of your own instance
   * @return the actual instance of api that will be used by your handlers during tests
   */
  override def mockApi(api: TestApi): TestApi = {
    val mockApi = stub[TestApi]

    inAnyOrder {
      (mockApi.queryDatabase _)
        .when(1337).returns(Some(validCar))
      (mockApi.queryDatabase _)
        .when(42).returns(None)
    }


    mockApi
  }

  implicit val showHandlerInstance: TestShow = new TestShow()

  "TestShow" must behave like handlerForShow(
    ShowTestCase.Success(id = 1337, expectedOutput = validCar),
    ShowTestCase.Fail(id = 42, expectedStatus = Some(404), caseDescription = Some("Return a 404 if no matching record is found"))
  )
}
