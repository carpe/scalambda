package io.carpe.scalambda.testing.api.fixtures

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.{APIGatewayProxyRequest, RequestContext, RequestContextIdentity}
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.testing.ScalambdaFixtures
import io.carpe.scalambda.testing.api.resourcehandlers.ApiResourceHandling
import io.circe.{Decoder, Encoder}

trait ApiScalambdaFixtures[C <: ScalambdaApi] extends ScalambdaFixtures {
  this: ApiResourceHandling[C] =>

  def makeTestRequestWithoutBody[O]
  (handler: ApiResource[C, None.type, APIGatewayProxyRequest.WithoutBody, O], queryParameters: Map[String, String] = Map.empty, pathParameters: Map[String, String] = Map.empty)
  (implicit encoderO: Encoder[O], decoderO: Decoder[O], requestContext: Context): APIGatewayProxyResponse = {
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

    handleApiResource(handler, apiGatewayReq, Encoder.encodeNone, encoderO, decoderO, requestContext)
  }

  def makeTestRequestWithBody[I, R <: APIGatewayProxyRequest.WithBody[I], O]
  (handler: ApiResource[C, I, R, O], body: I, pathParameters: Map[String, String] = Map.empty)
  (implicit inputEncoder: Encoder[I], decoder: Decoder[O], encoder: Encoder[O], requestContext: Context): APIGatewayProxyResponse = {
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

    handleApiResource(handler, apiGatewayReq, encoderI = inputEncoder, encoderO = encoder, decoder = decoder, requestContext = requestContext)
  }


}
