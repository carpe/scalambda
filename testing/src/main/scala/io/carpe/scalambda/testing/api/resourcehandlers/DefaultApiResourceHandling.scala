package io.carpe.scalambda.testing.api.resourcehandlers

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.testing.ScalambdaFixtures
import io.circe.{Decoder, Encoder}
import org.scalatest.TestSuite

trait DefaultApiResourceHandling[C <: ScalambdaApi] extends ApiResourceHandling[C] with ScalambdaFixtures {
  this: TestSuite =>

  def handleApiResource[I, R <: APIGatewayProxyRequest[I], O]
  (handler: ApiResource[C, I, R, O], request: R)(implicit encoderI: Encoder[R], decoder: Decoder[APIGatewayProxyResponse[O]], requestContext: Context): APIGatewayProxyResponse[O] = {
    val encoded = encoderI.apply(request)

    // create stream for handler input
    val serializedRequest = encoded.noSpaces.stripMargin

    testRequest(handler, serializedRequest)
  }
}
