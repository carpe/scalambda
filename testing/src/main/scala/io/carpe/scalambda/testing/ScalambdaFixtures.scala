package io.carpe.scalambda.testing

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}
import io.carpe.scalambda.Scalambda
import io.circe.{Encoder, Json}

trait ScalambdaFixtures[I, O] {

  def createHandler: Scalambda[I, O]

  val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

  def makeContext(functionName: String = "unit-test-function",
                  timeRemaining: Int = 5000,
                  lambdaLogger: LambdaLogger = UnitTestLogger,
                  functionVersion: String = "unit-test-function-version",
                  memoryLimit: Int = 1024,
                  clientContext: ClientContext = null,
                  logStreamName: String = "/unit-test",
                  invokedFunctionArn: String = "unit-test-arn",
                  cognitoIdentity: CognitoIdentity = null,
                  logGroupName: String = "unit-test-group",
                  awsReqId: String = "unit-test-request-id"): Context =
    new Context {
      override def getFunctionName: String = functionName
      override def getRemainingTimeInMillis: Int = timeRemaining
      override def getLogger: LambdaLogger = lambdaLogger
      override def getFunctionVersion: String = functionVersion
      override def getMemoryLimitInMB: Int = memoryLimit
      override def getClientContext: ClientContext = clientContext
      override def getLogStreamName: String = logStreamName
      override def getInvokedFunctionArn: String = functionVersion
      override def getIdentity: CognitoIdentity = cognitoIdentity
      override def getLogGroupName: String = logGroupName
      override def getAwsRequestId: String = awsReqId
    }

  def sendTestRequest(body: I)(implicit encoder: Encoder[I]): Json = {
    import io.circe.parser._

    val output        = new ByteArrayOutputStream()

    val serializedRequest = encoder(body).noSpaces.stripMargin

    createHandler.handler(streamFromString(serializedRequest), output, makeContext())

    parse(output.toString).right.get
  }

  def makeResponse(body: O)(implicit encoder: Encoder[O]): Json = {
    encoder(body)
  }

}
