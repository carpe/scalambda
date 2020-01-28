package io.carpe.scalambda.testing

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.request.RequestContext.{Authenticated, Unauthenticated}
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext, RequestContextIdentity}
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError}
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.TestSuite

trait ScalambdaFixtures extends LazyLogging { this: TestSuite =>

  import io.circe.generic.semiauto._

  private implicit lazy val requestContextEncoder: Encoder[RequestContext] = {
    case authed: Authenticated =>
      RequestContext.encodeAuthenticated.apply(authed)
    case unauthed: Unauthenticated =>
      RequestContext.encodeAnon.apply(unauthed)
  }

  protected implicit def proxyRequestWithBodyEncoder[R](implicit bodyEncoder: Encoder[R]): Encoder.AsObject[APIGatewayProxyRequest.WithBody[R]] = Encoder.forProduct10(
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
  case class IntermediaryProxyResponse(statusCode: Int, headers: Map[String, String] = Map.empty, body: Option[String], isBase64Encoded: Boolean = false)

  case class IntermediaryApiError(message: String) extends ApiError

  private implicit lazy val apiErrorDecoder: Decoder[IntermediaryApiError] = deriveDecoder[IntermediaryApiError]

  implicit def responseDecoder[T](implicit decoder: Decoder[T], encoder: Encoder[T]): Decoder[APIGatewayProxyResponse[T]] = {
    deriveDecoder[IntermediaryProxyResponse].map(intermediary => {
      import io.circe.parser._

      intermediary.body match {
        case Some(body) =>
          val parsed: Json = parse(body).fold(e => throw e, c => c)
          parsed.as[T].fold(err => {
            logger.debug("Could not parse body, attempting to parse as error...", err)
            parsed.as[IntermediaryApiError].fold(errErr => {
              logger.error("Could not parse body, attempting to parse as error...", errErr)
              fail("Could not serialize response to a record OR error. See logs for details.")
            }, intermediaryError => {
              APIGatewayProxyResponse.WithError(intermediary.headers, intermediaryError, intermediary.isBase64Encoded)
            })
          }, success => {
            APIGatewayProxyResponse.WithBody(intermediary.statusCode, intermediary.headers, success, intermediary.isBase64Encoded)
          })

        case None =>
          APIGatewayProxyResponse.Empty(intermediary.statusCode, intermediary.headers, intermediary.isBase64Encoded)
      }
    })

  }

  protected[testing] val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

  def makeTestRequestWithoutBody[O](handler: Scalambda[APIGatewayProxyRequest.WithoutBody, APIGatewayProxyResponse[O]])(implicit decoder: Decoder[O], encoder: Encoder[O], requestContext: Context): APIGatewayProxyResponse[O] = {
    val apiGatewayReq = APIGatewayProxyRequest.WithoutBody(
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
      None
    )

    val serializedRequest = deriveEncoder[APIGatewayProxyRequest.WithoutBody]
      .apply(apiGatewayReq)
      .noSpaces.stripMargin

    testApiRequest(handler, serializedRequest)
  }

  def makeTestRequestWithBody[I, O](handler: Scalambda[APIGatewayProxyRequest.WithBody[I], APIGatewayProxyResponse[O]], body: I)(implicit inputEncoder: Encoder[I], decoder: Decoder[O], encoder: Encoder[O], requestContext: Context): APIGatewayProxyResponse[O] = {
    val apiGatewayReq = APIGatewayProxyRequest.WithBody(
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
      Some(body),
      None
    )

    val serializedRequest = proxyRequestWithBodyEncoder[I]
      .apply(apiGatewayReq)
      .noSpaces.stripMargin

    testApiRequest(handler, serializedRequest)
  }

  def testApiRequest[I, O](handler: Scalambda[I, APIGatewayProxyResponse[O]], serializedRequest: String)(implicit decoder: Decoder[O], encoder: Encoder[O], requestContext: Context): APIGatewayProxyResponse[O] = {

    val testOutputStream  = new ByteArrayOutputStream()

    val testInput = streamFromString(serializedRequest)

    handler.handler(testInput, testOutputStream, requestContext)

    val testOutput = testOutputStream.toString

    import io.circe.parser._

    parse(testOutput).fold(
      err =>
        throw err
      , success =>
        success.as[APIGatewayProxyResponse[O]]

    ) match {
      case Left(value: DecodingFailure) =>
        throw value
      case Right(value) =>
        value
    }
  }
}
