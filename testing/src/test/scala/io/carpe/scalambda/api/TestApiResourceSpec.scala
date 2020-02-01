package io.carpe.scalambda.api

import io.carpe.scalambda.api.testapi.{TestApi, TestCreate, TestShow, TestUpdate}
import io.carpe.scalambda.testing.api.ApiResourceSpec
import io.carpe.scalambda.testing.api.behaviors.ApiResourceBehaviors.{CreateTestCase, ShowTestCase, UpdateTestCase}
import org.scalatest.flatspec.AnyFlatSpec

class TestApiResourceSpec extends AnyFlatSpec with ApiResourceSpec[TestApi] {

  import io.carpe.scalambda.fixtures.TestModels._

  // instance of TestCreate handler
  implicit val createHandlerInstance: TestCreate = new TestCreate()

  "TestCreate" should behave like handlerForCreate(
    CreateTestCase.Success(input = validCar, expectedOutput = validCar),
    CreateTestCase.Fail(lowHorsepowerCar, expectedMessage = Some("Not enough horsepower"), expectedStatus = Some(422), caseDescription = Some("validate records before creating them"))
  )

  // instance of TestUpdate handler
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
}