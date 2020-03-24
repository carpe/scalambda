package io.carpe.scalambda.testing.api.fixtures

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext, RequestContextIdentity}
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError, ApiErrors}
import io.carpe.scalambda.testing.ScalambdaFixtures
import io.carpe.scalambda.testing.api.resourcehandlers.ApiResourceHandling
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.TestSuite

trait ApiScalambdaFixtures[C <: ScalambdaApi] extends ScalambdaFixtures with ApiResourceHandling[C] {
  this: TestSuite =>

  def makeTestRequestWithoutBodyOrResponse
  (handler: ApiResource[C, Nothing, APIGatewayProxyRequest.WithoutBody, Nothing], queryParameters: Map[String, String] = Map.empty, pathParameters: Map[String, String] = Map.empty)
  (implicit requestContext: Context): APIGatewayProxyResponse[Nothing] = {
    val apiGatewayReq: APIGatewayProxyRequest.WithoutBody = APIGatewayProxyRequest.WithoutBody(
      "/resource",
      "/unit-test",
      "POST",
      Map.empty,
      queryParameters,
      pathParameters,
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

    implicit def responseDecoder: Decoder[APIGatewayProxyResponse[Nothing]] = {
      deriveDecoder[IntermediaryProxyResponse].map(intermediary => {
        import io.circe.parser._

        intermediary.body match {
          case Some(body) =>
            // parse the response
            val parsed: Json = parse(body).fold(e => throw e, c => c)

            // decode the response into either an ApiError or api response
            apiErrorDecoder.decodeJson(parsed).fold(
              decoderFailure =>
                fail("Could not serialize response to a record OR error. ", decoderFailure),
              responseErr =>
                // build proper response from parsed error
                APIGatewayProxyResponse.WithError(intermediary.headers, ApiErrors(new ApiError {
                  override val httpStatus: Int = intermediary.statusCode
                  override val headers: Map[String, String] = intermediary.headers
                  override val message: String = responseErr.message
                  override val errorCode: Option[Int] = responseErr.errorCode
                  override val additional: Option[Json] = responseErr.additional
                }), intermediary.isBase64Encoded)
            )

          case None =>
            APIGatewayProxyResponse.Empty(intermediary.statusCode, intermediary.headers, intermediary.isBase64Encoded)
        }
      })

    }

    handleApiResource[Nothing, APIGatewayProxyRequest.WithoutBody, Nothing](handler, apiGatewayReq)
  }

  def makeTestRequestWithoutBody[O]
  (handler: ApiResource[C, Nothing, APIGatewayProxyRequest.WithoutBody, O], queryParameters: Map[String, String] = Map.empty, pathParameters: Map[String, String] = Map.empty)
  (implicit encoderI: Encoder[APIGatewayProxyRequest.WithoutBody], decoderO: Decoder[APIGatewayProxyResponse[O]], requestContext: Context): APIGatewayProxyResponse[O] = {
    val apiGatewayReq = APIGatewayProxyRequest.WithoutBody(
      "/resource",
      "/unit-test",
      "POST",
      Map.empty,
      queryParameters,
      pathParameters,
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

    handleApiResource(handler, apiGatewayReq)
  }

  def makeTestRequestWithBody[I, R <: APIGatewayProxyRequest.WithBody[I], O]
  (handler: ApiResource[C, I, R, O], body: I, pathParameters: Map[String, String] = Map.empty)
  (implicit inputEncoder: Encoder[R], decoder: Decoder[APIGatewayProxyResponse[O]], requestContext: Context): APIGatewayProxyResponse[O] = {
    val apiGatewayReq = APIGatewayProxyRequest.WithBody(
      "/resource",
      "/unit-test",
      "POST",
      Map.empty,
      Map.empty,
      pathParameters,
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
    ).asInstanceOf[R]

    handleApiResource(handler, apiGatewayReq)
  }


}
