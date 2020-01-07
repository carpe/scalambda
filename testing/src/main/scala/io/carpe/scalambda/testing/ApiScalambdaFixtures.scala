package io.carpe.scalambda.testing

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext, RequestContextIdentity}
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

trait ApiScalambdaFixtures[I, O] {

  def createHandler: Scalambda[I, O]

  protected implicit val requestContextEncoder: Encoder[RequestContext] = deriveEncoder[RequestContext]

  protected def proxyRequestEncoder(implicit bodyEncoder: Encoder[I]): Encoder.AsObject[APIGatewayProxyRequest[I]] = Encoder.forProduct10(
    "resource",
    "path",
    "httpMethod",
    "headers",
    "queryStringParameters",
    "pathParameters",
    "stageVariables",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(response => {
    import io.circe.syntax._

    ( response.resource, response.path, response.httpMethod, response.headers, response.queryStringParameters,
      response.pathParameters, response.stageVariables, response.requestContext, response.body.asJson.noSpaces,
      response.isBase64Encoded
    )
  })

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

  def sendTestRequest(implicit bodyEncoder: Encoder[I]): Json = {
    sendTestRequest(None)
  }

  def sendTestRequest(body: I)(implicit bodyEncoder: Encoder[I]): Json = {
    sendTestRequest(Some(body))
  }

  def sendTestRequest(maybeBody: Option[I])(implicit bodyEncoder: Encoder[I]): Json = {
    import io.circe.parser._

    val output        = new ByteArrayOutputStream()

    val apiGatewayReq = APIGatewayProxyRequest(
      "/resource",
      "/unit-test",
      "POST",
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      RequestContext.Unauthenticated(None,
        None,
        None,
        "unit",
        None,
        RequestContextIdentity(None, None, None, None, None, "127.0.0.1", None, None, None, None, None),
        "/unit-test-path",
        "POST",
        None
      ),
      maybeBody,
      None
    )

    val serializedRequest = apiGatewayReq.asJson(proxyRequestEncoder).noSpaces

    createHandler.handler(streamFromString(serializedRequest), output, makeContext())

    parse(output.toString).right.get
  }


  def makeResponse(body: O, responseCode: Int)(implicit encoder: Encoder[O]): Json = {
    val responseBody = encoder(body).noSpaces.stripMargin

    val response: APIGatewayProxyResponse[String] = APIGatewayProxyResponse.WithBody[String](
      statusCode = responseCode,
      headers = Map("Access-Control-Allow-Origin" -> "*"),
      body = responseBody,
      isBase64Encoded = false
    )

    response.asJson(APIGatewayProxyResponse.encoder[String])
  }

}