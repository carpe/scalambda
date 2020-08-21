package io.carpe.scalambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}
import io.circe.Encoder

trait ScalambdaFixtures {

  def createTestOutputStream(): OutputStream = {
    // create output buffer to capture lambda output in
     new ByteArrayOutputStream()
  }

  def createTestInputStream[I](input: I)(implicit encoder: Encoder[I]): InputStream = {
    // serialize the input
    val serializedRequest = encoder(input).noSpaces.stripMargin
    // send input to input stream so it can be read by test lambda instance
    new ByteArrayInputStream(serializedRequest.getBytes)
  }

  val testContext: Context = new Context {
    override def getAwsRequestId: String = ???

    override def getLogGroupName: String = ???

    override def getLogStreamName: String = ???

    override def getFunctionName: String = ???

    override def getFunctionVersion: String = ???

    override def getInvokedFunctionArn: String = ???

    override def getIdentity: CognitoIdentity = ???

    override def getClientContext: ClientContext = ???

    override def getRemainingTimeInMillis: Int = ???

    override def getMemoryLimitInMB: Int = ???

    override def getLogger: LambdaLogger = ???
  }
}
