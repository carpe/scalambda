package io.carpe.scalambda.api.show

import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.api.ApiResourceInput
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.{ApiError, ApiErrors}

case class ShowRequest(id: Int)(val original: APIGatewayProxyRequest[None.type]) extends ApiResourceInput[None.type]

object ShowRequest extends LazyLogging {

  def fromProxyRequest(proxyRequest: APIGatewayProxyRequest[None.type]): Either[ApiErrors, ShowRequest] = {
    proxyRequest.pathParameters.get("id").map(id => {
      try {
        val withParsedId = ShowRequest(id = Integer.parseInt(id))(proxyRequest)
        Right(withParsedId)
      } catch {
        case e: NumberFormatException =>
          logger.error("`id` path parameter for show request was not an Integer. String path parameters are not currently supported.", e)
          Left(ApiErrors(ApiError.InternalError))
      }
    }).getOrElse({
      logger.error("Missing `id` path parameter for show request. Please add `id` as a path parameter!")
      Left(ApiErrors(ApiError.InternalError))
    })
  }
}
