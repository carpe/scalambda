package io.carpe.scalambda.testing.api.resourcehandlers

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.circe.{Decoder, Encoder}
import org.scalatest.TestSuite

trait ApiResourceHandling[C <: ScalambdaApi] {
  this: TestSuite =>

  def handleApiResource[I, R <: APIGatewayProxyRequest[I], O](handler: ApiResource[C, I, R, O], request: R, encoderI: Encoder[I], encoderO: Encoder[O], decoder: Decoder[O], requestContext: Context): APIGatewayProxyResponse

}
