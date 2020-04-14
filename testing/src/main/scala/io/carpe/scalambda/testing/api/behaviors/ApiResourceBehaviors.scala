package io.carpe.scalambda.testing.api.behaviors

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.testing.MockContext
import io.carpe.scalambda.testing.api.fixtures.ApiScalambdaFixtures
import io.carpe.scalambda.testing.api.resourcehandlers.ApiResourceHandling
import io.circe.{Decoder, Encoder}
import org.scalatest.flatspec.AnyFlatSpec

trait ApiResourceBehaviors[C <: ScalambdaApi] extends ApiScalambdaFixtures[C] with ApiResourceHandling[C] {
  this: AnyFlatSpec =>

  import ApiResourceBehaviors._

  /**
   * Allows for simple creation of tests for Create handlers
   */
  def handlerForCreate[R](testCase: CreateTestCase[R], testCases: CreateTestCase[R]*)
                         (implicit handler: ApiResource.Create[C, R],
                          typeEncoder: Encoder[APIGatewayProxyRequest.WithBody[R]],
                          typeDecoder: Decoder[APIGatewayProxyResponse[R]],
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
                expectedStatus.foreach(expected => assert(err.errors.head.httpStatus === expected))
                expectedMessage.foreach(expected => assert(err.errors.head.message === expected))
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            fail("Show failed to return a body.")

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case CreateTestCase.Success(input, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: CreateTestCase.Fail[R] =>
                fail("Expected failure but the handler completed successfully.")
            }
        }
      }
    })
  }

  /**
   * Allows for simple creation of tests for Index handlers
   */
  def handlerForIndex[R](testCase: IndexTestCase, testCases: IndexTestCase*)
                        (implicit handler: ApiResource.Index[C, R],
                         typeEncoder: Encoder[R],
                         typeDecoder: Decoder[APIGatewayProxyResponse[R]],
                         requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[IndexTestCase] = testCase +: testCases

    cases.foreach(test => {
      it should createTestDescription(test) in {
        makeTestRequestWithoutBody(handler, queryParameters = test.queryParameters) match {
          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
            test match {
              case IndexTestCase.Success(queryParameters, expectedOutput, caseDescription) =>
                fail(s"Expected success, but the handler failed. Response was: ${err}")

              case IndexTestCase.Fail(queryParameters, expectedMessage, expectedStatus, caseDescription) =>
                expectedStatus.foreach(expected => assert(err.errors.head.httpStatus === expected))
                expectedMessage.foreach(expected => assert(err.errors.head.message === expected))
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            fail("Show failed to return a body.")

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case IndexTestCase.Success(queryParameters, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: IndexTestCase.Fail =>
                fail("Expected failure but the handler completed successfully.")
            }
        }
      }
    })
  }

  /**
   * Allows for simple creation of tests for Show handlers
   */
  def handlerForShow[R](testCase: ShowTestCase, testCases: ShowTestCase*)
                       (implicit handler: ApiResource.Show[C, R],
                        typeEncoder: Encoder[R],
                        typeDecoder: Decoder[APIGatewayProxyResponse[R]],
                        requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[ShowTestCase] = testCase +: testCases

    cases.foreach(test => {
      it should createTestDescription(test) in {
        makeTestRequestWithoutBody(handler, pathParameters = Map("id" -> test.id.toString)) match {
          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
            test match {
              case ShowTestCase.Success(id, expectedOutput, caseDescription) =>
                fail(s"Expected success, but the handler failed. Response was: ${err}")

              case ShowTestCase.Fail(id, expectedMessage, expectedStatus, caseDescription) =>
                expectedStatus.foreach(expected => assert(err.errors.head.httpStatus === expected))
                expectedMessage.foreach(expected => assert(err.errors.head.message === expected))
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            fail("Show failed to return a body.")

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case ShowTestCase.Success(id, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: ShowTestCase.Fail =>
                fail("Expected failure but the handler completed successfully.")
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
                          typeEncoder: Encoder[APIGatewayProxyRequest.WithBody[R]],
                          typeDecoder: Decoder[APIGatewayProxyResponse[R]],
                          requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[UpdateTestCase[R]] = testCase +: testCases

    cases.foreach(test => {
      it should createTestDescription(test) in {
        makeTestRequestWithBody(handler, test.input, pathParameters = Map("id" -> test.id.toString)) match {
          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
            test match {
              case UpdateTestCase.Success(id, input, expectedOutput, caseDescription) =>
                fail(s"Expected success, but the handler failed. Response was: ${err}")

              case UpdateTestCase.Fail(id, input, expectedMessage, expectedStatus, caseDescription) =>
                expectedStatus.foreach(expected => assert(err.errors.head.httpStatus === expected))
                expectedMessage.foreach(expected => assert(err.errors.head.message === expected))
            }


          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
            fail("Update failed to return a body.")

          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
            test match {
              case UpdateTestCase.Success(id, input, expectedOutput, caseDescription) =>
                assert(body === expectedOutput)
              case _: UpdateTestCase.Fail[R] =>
                fail("Expected failure but the handler completed successfully.")
            }
        }
      }
    })
  }

  /**
   * Allows for simple creation of tests for Update handlers
   */
  def handlerForDelete(testCase: DeleteTestCase, testCases: DeleteTestCase*)
                      (implicit handler: ApiResource.Delete[C],
                       requestContext: Context = MockContext.default): Unit = {
    val cases: Seq[DeleteTestCase] = testCase +: testCases

//    cases.foreach(test => {
//      it should createTestDescription(test) in {
//        makeTestRequestWithoutBody(handler, pathParameters = Map("id" -> test.id.toString)) match {
//          case APIGatewayProxyResponse.WithError(headers, err, isBase64Encoded) =>
//            test match {
//              case DeleteTestCase.Success(id, caseDescription) =>
//                fail(s"Expected success, but the handler failed. Response was: ${err}")
//
//              case DeleteTestCase.Fail(id, expectedMessage, expectedStatus, caseDescription) =>
//                expectedStatus.foreach(expected => assert(err.httpStatus === expected))
//                expectedMessage.foreach(expected => assert(err.message === expected))
//            }
//
//
//          case APIGatewayProxyResponse.Empty(statusCode, headers, isBase64Encoded) =>
//            test match {
//              case DeleteTestCase.Success(id, caseDescription) =>
//
//              case DeleteTestCase.Fail(id, expectedMessage, expectedStatus, caseDescription) =>
//            }
//
//          case APIGatewayProxyResponse.WithBody(statusCode, headers, body, isBase64Encoded) =>
//            fail("Delete method returned a body... Which shouldn't be possible....")
//        }
//      }
//    })
  }


}

object ApiResourceBehaviors {

  private def createTestDescription(test: TestCase): String = {
    test.caseDescription match {
      case Some(value) =>
        value
      case None =>
        test match {
          case fail: FailCase =>
            fail.failureDefaultDescription
          case testCase: TestCase =>
            testCase.defaultDescription
        }
    }
  }

  sealed trait TestCase {
    def defaultDescription: String

    def caseDescription: Option[String]
  }

  sealed trait FailCase {
    this: TestCase =>
    def failureDefaultDescription: String = s"return errors when provided invalid update for ${this.defaultDescription}"
  }

  sealed trait CreateTestCase[I] extends TestCase {
    override def defaultDescription: String = "creation of a record"

    def input: I

    def caseDescription: Option[String]
  }

  object CreateTestCase {

    case class Success[R](input: R, expectedOutput: R, caseDescription: Option[String] = None) extends CreateTestCase[R]

    case class Fail[R](input: R, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends CreateTestCase[R] with FailCase

  }

  sealed trait IndexTestCase extends TestCase {
    override def defaultDescription: String = "fetching of records"

    def queryParameters: Map[String, String]

    def caseDescription: Option[String]
  }

  object IndexTestCase {

    case class Success[R](queryParameters: Map[String, String], expectedOutput: R, caseDescription: Option[String] = None) extends IndexTestCase

    case class Fail(queryParameters: Map[String, String], expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends IndexTestCase with FailCase

  }

  sealed trait ShowTestCase extends TestCase {
    override def defaultDescription: String = "updating of a record"

    def id: Int

    def caseDescription: Option[String]
  }

  object ShowTestCase {

    case class Success[R](id: Int, expectedOutput: R, caseDescription: Option[String] = None) extends ShowTestCase

    case class Fail(id: Int, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends ShowTestCase with FailCase

  }


  sealed trait UpdateTestCase[R] extends TestCase {
    override def defaultDescription: String = "updating of a record"

    def id: Int

    def input: R

    def caseDescription: Option[String]
  }

  object UpdateTestCase {

    case class Success[R](id: Int, input: R, expectedOutput: R, caseDescription: Option[String] = None) extends UpdateTestCase[R]

    case class Fail[R](id: Int, input: R, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends UpdateTestCase[R] with FailCase

  }

  sealed trait DeleteTestCase extends TestCase {
    override def defaultDescription: String = "deleting of a record"

    def id: Int

    def caseDescription: Option[String]
  }

  object DeleteTestCase {

    case class Success(id: Int, caseDescription: Option[String] = None) extends DeleteTestCase

    case class Fail(id: Int, expectedMessage: Option[String] = None, expectedStatus: Option[Int] = None, caseDescription: Option[String] = None) extends DeleteTestCase with FailCase

  }

}