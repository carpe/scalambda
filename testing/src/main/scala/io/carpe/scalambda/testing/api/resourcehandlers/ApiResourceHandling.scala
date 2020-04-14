package io.carpe.scalambda.testing.api.resourcehandlers

import cats.data.{Chain, NonEmptyChain}
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.RequestContext.{Authenticated, Unauthenticated}
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext}
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError, ApiErrors}
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.TestSuite

trait ApiResourceHandling[C <: ScalambdaApi] {
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
  case class IntermediaryApiErrors(errors: Seq[IntermediaryApiError])
  implicit lazy val apiErrorDecoder: Decoder[IntermediaryApiError] = deriveDecoder[IntermediaryApiError]
  implicit lazy val apiErrorsDecoder: Decoder[IntermediaryApiErrors] = deriveDecoder[IntermediaryApiErrors]

  implicit def responseDecoder[T](implicit decoder: Decoder[T], encoder: Encoder[T]): Decoder[APIGatewayProxyResponse[T]] = {
    deriveDecoder[IntermediaryProxyResponse].map(intermediary => {
      import io.circe.parser._

      val errorsOrSuccessDecoder: Decoder[Either[IntermediaryApiErrors, T]] = decoder.map(Right(_)) or apiErrorsDecoder.map(Left(_))

      intermediary.body match {
        case Some(body) =>
          // parse the response
          val parsed: Json = parse(body).fold(e => throw e, c => c)

          // decode the response into either an ApiError or api response
          errorsOrSuccessDecoder.decodeJson(parsed).fold(
            decodeFailure =>
              fail("Could not serialize response to a record OR error. ", decodeFailure),
            {
              case Left(responseErrs) =>
                // build proper response from parsed error
                val apiErrorSeq: Seq[ApiError] = responseErrs.errors.map(responseErr => {
                  new ApiError {
                    override val httpStatus: Int = intermediary.statusCode
                    override val headers: Map[String, String] = intermediary.headers
                    override val message: String = responseErr.message
                    override val errorCode: Option[Int] = responseErr.errorCode
                    override val additional: Option[Json] = responseErr.additional
                  }
                })

                val apiErrors = NonEmptyChain.fromChain[ApiError](Chain.fromSeq(apiErrorSeq)).map(safeChain => {
                  ApiErrors(safeChain)
                }).getOrElse(fail("Empty errors object. This shouldn't be possible"))



                APIGatewayProxyResponse.WithError(intermediary.headers, apiErrors, intermediary.isBase64Encoded)
              case Right(responseBody) =>
                APIGatewayProxyResponse.WithBody(intermediary.statusCode, intermediary.headers, responseBody, intermediary.isBase64Encoded)
            }
          )

        case None =>
          APIGatewayProxyResponse.Empty(intermediary.statusCode, intermediary.headers, intermediary.isBase64Encoded)
      }
    })

  }

  def handleApiResource[I, R <: APIGatewayProxyRequest[I], O]
  (handler: ApiResource[C, I, R, O], request: R)(implicit encoderI: Encoder[R], decoder: Decoder[APIGatewayProxyResponse[O]], requestContext: Context): APIGatewayProxyResponse[O]

}
