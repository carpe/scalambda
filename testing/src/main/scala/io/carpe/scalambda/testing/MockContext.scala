package io.carpe.scalambda.testing

import java.util

import com.amazonaws.services.lambda.runtime._

case class MockContext(functionName: String = "unit-test-function",
                       timeRemaining: Int = 5000,
                       lambdaLogger: LambdaLogger = UnitTestLogger,
                       functionVersion: String = "unit-test-function-version",
                       memoryLimit: Int = 1024,
                       clientContext: ClientContext = MockContext.defaultClientContext,
                       logStreamName: String = "/unit-test",
                       invokedFunctionArn: String = "unit-test-arn",
                       cognitoIdentity: CognitoIdentity = MockContext.defaultCognitoIdentity,
                       logGroupName: String = "unit-test-group",
                       awsReqId: String = "unit-test-request-id") {
  def asLambdaContext: Context = {
    new Context {
      override def getFunctionName: String = functionName

      override def getRemainingTimeInMillis: Int = timeRemaining

      override def getLogger: LambdaLogger = lambdaLogger

      override def getFunctionVersion: String = functionVersion

      override def getMemoryLimitInMB: Int = memoryLimit

      override def getClientContext: ClientContext = clientContext

      override def getLogStreamName: String = logStreamName

      override def getInvokedFunctionArn: String = invokedFunctionArn

      override def getIdentity: CognitoIdentity = cognitoIdentity

      override def getLogGroupName: String = logGroupName

      override def getAwsRequestId: String = awsReqId
    }
  }
}

object MockContext {

  @deprecated(message = "Use the MockContext case class or `MockContext.default` instead", since = "5.1.0")
  def makeContext(functionName: String = "unit-test-function",
                  timeRemaining: Int = 5000,
                  lambdaLogger: LambdaLogger = UnitTestLogger,
                  functionVersion: String = "unit-test-function-version",
                  memoryLimit: Int = 1024,
                  clientContext: ClientContext = defaultClientContext,
                  logStreamName: String = "/unit-test",
                  invokedFunctionArn: String = "unit-test-arn",
                  cognitoIdentity: CognitoIdentity = defaultCognitoIdentity,
                  logGroupName: String = "unit-test-group",
                  awsReqId: String = "unit-test-request-id"): Context = {
    MockContext(functionName, timeRemaining, lambdaLogger, functionVersion, memoryLimit, clientContext, logStreamName,
      invokedFunctionArn, cognitoIdentity, logGroupName, awsReqId
    ).asLambdaContext
  }


  lazy val defaultClientContext: ClientContext = new ClientContext {
    override def getClient: Client = new Client {
      override def getInstallationId: String = "InstallationId"

      override def getAppTitle: String = "AppTitle"

      override def getAppVersionName: String = "AppVersionName"

      override def getAppVersionCode: String = "AppVersionCode"

      override def getAppPackageName: String = "AppPackageName"
    }

    override def getCustom: util.Map[String, String] = new util.WeakHashMap[String, String] {}

    override def getEnvironment: util.Map[String, String] = new util.WeakHashMap[String, String] {}
  }

  lazy val defaultCognitoIdentity: CognitoIdentity = new CognitoIdentity {
    override def getIdentityId: String = "someid"

    override def getIdentityPoolId: String = "someidpool"
  }

  lazy val default: Context = new MockContext().asLambdaContext
}
