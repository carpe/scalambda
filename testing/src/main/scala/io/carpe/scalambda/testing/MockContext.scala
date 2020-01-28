package io.carpe.scalambda.testing

import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}

object MockContext {
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

  lazy val default: Context = makeContext()
}
