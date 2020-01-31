package io.carpe.scalambda.testing

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.testing.ApiResourceBehaviors.{CreateTestCase, UpdateTestCase, createTestDescription}
import io.carpe.scalambda.testing.api.resourcehandlers.ApiResourceHandling
import io.circe.{Decoder, Encoder}
import org.scalatest.flatspec.AnyFlatSpec

trait ApiResourceBehaviors[C <: ScalambdaApi] extends ApiScalambdaFixtures[C] with ApiResourceHandling[C] {
  this: AnyFlatSpec =>

  /**
   * Allows for simple creation of tests for Create handlers
   */
  def handlerForCreate[R](testCase: CreateTestCase[R], testCases: CreateTestCase[R]*)
                         (implicit handler: ApiResource.Create[C, R],
                          typeEncoder: Encoder[R],
                          typeDecoder: Decoder[R],
                          requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[CreateTestCase[R]] = testCase +: testCases

    cases.foreach(test => {
      it should createTestDescription(test) in {
        makeTestRequestWithBody(handler, test.input) match {
          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
            test match {
              case CreateTestCase.Success(input, expectedOutput, caseDescription) =>
                fail(s"Expected success, but the handler failed. Response was: ${err}")

              case CreateTestCase.Fail(input, expectedMessage, expectedStatus, caseDescription) =>
                expectedStatus.foreach(expected => err.httpStatus === expected)
                expectedMessage.foreach(expected => err.message === expected)
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            ???

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case CreateTestCase.Success(input, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: CreateTestCase.Fail[R] =>
                fail(s"Expected failure but the handler completed successfully.")
            }
        }
      }
    })
  }

  /**
   * Allows for simple creation of tests for Update handlers
   */
  def handlerForUpdate[R](testCase: UpdateTestCase[R], testCases: UpdateTestCase[R]*)
                         (implicit handler: ApiResource.Update[C, R],
                          typeEncoder: Encoder[R],
                          typeDecoder: Decoder[R],
                          requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[UpdateTestCase[R]] = testCase +: testCases

    cases.foreach(test => {
      it should createTestDescription(test) in {
        makeTestRequestWithBody(handler, test.input, pathParameters = Map("id" -> "1337")) match {
          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
            test match {
              case UpdateTestCase.Success(input, expectedOutput, caseDescription) =>
                fail(s"Expected success, but the handler failed. Response was: ${err}")

              case UpdateTestCase.Fail(input, expectedMessage, expectedStatus, caseDescription) =>
                expectedStatus.foreach(expected => err.httpStatus === expected)
                expectedMessage.foreach(expected => err.message === expected)
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            ???

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case UpdateTestCase.Success(input, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: UpdateTestCase.Fail[R] =>
                fail(s"Expected failure but the handler completed successfully.")
            }
        }
      }
    })
  }

}

object ApiResourceBehaviors {

  private def createTestDescription[I](test: TestCase[I]): String = {
    test.caseDescription match {
      case Some(value) =>
        value
      case None =>
        test match {
          case fail: FailCase =>
            fail.failureDefaultDescription
          case testCase: TestCase[_] =>
            testCase.defaultDescription
        }
    }
  }

  sealed trait TestCase[I] {
    def defaultDescription: String

    def caseDescription: Option[String]
  }

  sealed trait FailCase {
    this: TestCase[_] =>
    def failureDefaultDescription: String = s"return errors when provided invalid update for ${this.defaultDescription}"
  }

  sealed trait CreateTestCase[I] extends TestCase[I] {
    override def defaultDescription: String = "creation of a record"

    def input: I

    def caseDescription: Option[String]
  }

  object CreateTestCase {

    case class Success[R](input: R, expectedOutput: R, caseDescription: Option[String] = None) extends CreateTestCase[R]

    case class Fail[R](input: R, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends CreateTestCase[R] with FailCase

  }

  sealed trait UpdateTestCase[R] extends TestCase[R] {
    override def defaultDescription: String = "updating of a record"

    def input: R

    def caseDescription: Option[String]
  }

  object UpdateTestCase {

    case class Success[R](input: R, expectedOutput: R, caseDescription: Option[String] = None) extends UpdateTestCase[R]

    case class Fail[R](input: R, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends UpdateTestCase[R] with FailCase

  }

}