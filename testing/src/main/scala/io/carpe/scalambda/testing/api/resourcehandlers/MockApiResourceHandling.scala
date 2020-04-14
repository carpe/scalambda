package io.carpe.scalambda.testing.api.resourcehandlers

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.ApiResource.defaultResponseHeaders
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.circe.{Decoder, Encoder}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite

trait MockApiResourceHandling[C <: ScalambdaApi] extends ApiResourceHandling[C] with MockFactory {
  this: TestSuite =>

  /**
   * You can use this function to inject mocks into your ApiResource handlers.
   *
   * @param api which you can manipulate or omit entirely in favor of your own instance
   * @return the actual instance of api that will be used by your handlers during tests
   */
  def mockApi(api: C): C

  override def handleApiResource[I, R <: APIGatewayProxyRequest[I], O]
  (handler: ApiResource[C, I, R, O], request: R)(implicit encoderI: Encoder[R], decoder: Decoder[APIGatewayProxyResponse[O]], requestContext: Context): APIGatewayProxyResponse[O] = {

    // inject mocks into the resource's bootstrapping
    val bootstrap = handler.init(requestContext)
    implicit val bootstrapWithMocks: C = mockApi(bootstrap)

    handler.handleApiRequest(request)
      .fold(errors => APIGatewayProxyResponse.WithError(defaultResponseHeaders, errors), identity)
  }
}
