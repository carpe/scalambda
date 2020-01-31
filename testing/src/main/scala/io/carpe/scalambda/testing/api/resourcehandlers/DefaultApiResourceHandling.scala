package io.carpe.scalambda.testing.api.resourcehandlers

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.RequestContext.{Authenticated, Unauthenticated}
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext}
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError}
import io.carpe.scalambda.testing.ScalambdaFixtures
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.TestSuite

trait DefaultApiResourceHandling[C <: ScalambdaApi] extends ApiResourceHandling[C] with ScalambdaFixtures {
  this: TestSuite =>

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

    (response.resource, response.path, response.httpMethod, response.headers, response.queryStringParameters,
      response.pathParameters, response.stageVariables, response.requestContext, response.body.asJson.noSpaces,
      response.isBase64Encoded
    )
  })

  protected implicit def proxyRequestWithoutBodyEncoder: Encoder.AsObject[APIGatewayProxyRequest.WithoutBody] = Encoder.forProduct9(
    "resource",
    "path",
    "httpMethod",
    "headers",
    "queryStringParameters",
    "pathParameters",
    "stageVariables",
    "requestContext",
    "isBase64Encoded"
  )(response => {

    (response.resource, response.path, response.httpMethod, response.headers, response.queryStringParameters,
      response.pathParameters, response.stageVariables, response.requestContext,
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

  override def handleApiResource[I, R <: APIGatewayProxyRequest[I], O]
  (handler: ApiResource[C, I, R, O], apiGatewayReq: R)
  (implicit encoderI: Encoder[I], encoderO: Encoder[O], decoder: Decoder[O], requestContext: Context): APIGatewayProxyResponse[O] = {
    val encoded = apiGatewayReq match {
      case withBody: APIGatewayProxyRequest.WithBody[I] =>
        proxyRequestWithBodyEncoder[I].apply(withBody)
      case withoutBody: APIGatewayProxyRequest.WithoutBody =>
        proxyRequestWithoutBodyEncoder(withoutBody)
    }

    // create stream for handler input
    val serializedRequest = encoded.noSpaces.stripMargin

    testRequest(handler, serializedRequest)
  }
}
