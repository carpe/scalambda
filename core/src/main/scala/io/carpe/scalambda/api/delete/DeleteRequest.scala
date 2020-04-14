package io.carpe.scalambda.api.delete

import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.api.ApiResourceInput
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.{ApiError, ApiErrors}

case class DeleteRequest(id: Int)(val original: APIGatewayProxyRequest.WithoutBody) extends ApiResourceInput[Nothing]

object DeleteRequest extends LazyLogging {
  def fromProxyRequest(proxyRequest: APIGatewayProxyRequest.WithoutBody): Either[ApiErrors, DeleteRequest] = {
    proxyRequest.pathParameters.get("id").map(id => {
      try {
        val withParsedId = DeleteRequest(id = Integer.parseInt(id))(proxyRequest)
        Right(withParsedId)
      } catch {
        case e: NumberFormatException =>
          logger.error("`id` path parameter for delete request was not an Integer. String path parameters are not currently supported.", e)
          Left(ApiErrors(ApiError.InternalError))
      }
    }).getOrElse({
      logger.error("Missing `id` path parameter for delete request. Please add `id` as a path parameter!")
      Left(ApiErrors(ApiError.InternalError))
    })
  }
}


