package io.carpe.scalambda.api

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.testapi.TestIndex.TestIndexResponse
import io.carpe.scalambda.api.testapi.{TestApi, TestCreate, TestDelete, TestIndex, TestShow, TestUpdate}
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.testing.MockContext
import io.carpe.scalambda.testing.api.ApiResourceSpec
import io.carpe.scalambda.testing.api.behaviors.ApiResourceBehaviors.{CreateTestCase, DeleteTestCase, IndexTestCase, ShowTestCase, UpdateTestCase}
import org.scalatest.flatspec.AnyFlatSpec

class TestApiResourceSpec extends AnyFlatSpec with ApiResourceSpec[TestApi] {

  import io.carpe.scalambda.fixtures.TestModels._

  // instance of TestCreate handler
  implicit val createHandlerInstance: TestCreate = new TestCreate()

  "TestCreate" should behave like handlerForCreate(
    CreateTestCase.Success(input = validCar, expectedOutput = validCar),
    CreateTestCase.Fail(lowHorsepowerCar, expectedMessage = Some("Not enough horsepower"), expectedStatus = Some(422), caseDescription = Some("validate records before creating them"))
  )

  // instance of TestIndex handler
  implicit val indexHandlerInstance: TestIndex = new TestIndex()

  "TestIndex" should behave like handlerForIndex(
    IndexTestCase.Success(Map.empty, TestIndexResponse(List.empty))
  )

  // instance of TestShow handler
  implicit val showHandlerInstance: TestShow = new TestShow()

  "TestShow" should behave like handlerForShow(
    ShowTestCase.Fail(id = 1337, expectedStatus = Some(500), caseDescription = Some("should require it's database to be mocked"))
  )

  // instance of TestUpdate handler
  implicit val updateHandlerInstance: TestUpdate = new TestUpdate()

  "TestUpdate" should behave like handlerForUpdate(
    UpdateTestCase.Success(id = 1337, input = validCar, expectedOutput = validCar.copy(hp = 1337)),
    UpdateTestCase.Fail(lowHorsepowerCar.hp, lowHorsepowerCar, expectedMessage = Some("Not enough horsepower"), expectedStatus = Some(422), caseDescription = Some("validate records before updating them"))
  )

  // instance of TestDelete handler
  implicit val deleteHandlerInstance: TestDelete = new TestDelete()

  "TestDelete" should behave like handlerForDelete(
    DeleteTestCase.Success(id = 1337)
  )

  it should "be testable via included test helpers" in {
    implicit val requestContext: Context = MockContext.default

    val response = makeTestRequestWithoutBodyOrResponse(deleteHandlerInstance, pathParameters = Map("id" -> "1337"))

    response match {
      case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
      case other: APIGatewayProxyResponse[_] =>
        fail(s"Response was ${other}, but should have been Empty.")
    }
  }
}