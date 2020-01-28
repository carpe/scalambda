package io.carpe.scalambda.api

import io.carpe.scalambda.api.api.{TestCreate, TestUpdate}
import io.carpe.scalambda.testing.ApiResourceBehaviors
import io.carpe.scalambda.testing.ApiResourceBehaviors.{CreateTestCase, UpdateTestCase}
import org.scalatest.flatspec.AnyFlatSpec

class ApiResourceSpec extends AnyFlatSpec with ApiResourceBehaviors {

  import io.carpe.scalambda.fixtures.TestModels._

  // instance of TestCreate handler
  implicit val createHandlerInstance: TestCreate = TestCreate()

  "TestCreate" should behave like handlerForCreate(
    CreateTestCase.Success(validCar, validCar),
    CreateTestCase.Fail(lowHorsepowerCar, expectedMessage = Some("Not enough horsepower"), expectedStatus = Some(422), caseDescription = Some("validate records before creating them"))
  )

  // instance of TestUpdate handler
  implicit val updateHandlerInstance: TestUpdate = TestUpdate()

  "TestUpdate" should behave like handlerForUpdate(
    UpdateTestCase.Success(validCar, validCar.copy(hp = 1337)),
    UpdateTestCase.Fail(lowHorsepowerCar, expectedMessage = Some("Not enough horsepower"), expectedStatus = Some(422), caseDescription = Some("validate records before updating them"))
  )
}