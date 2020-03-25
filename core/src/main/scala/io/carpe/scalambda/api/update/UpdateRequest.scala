package io.carpe.scalambda.api.update

import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.api.ApiResourceInput
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.{ApiError, ApiErrors}

case class UpdateRequest[R](id: Int, body: R)(val original: APIGatewayProxyRequest.WithBody[R]) extends ApiResourceInput[R]

object UpdateRequest extends LazyLogging {
  def fromProxyRequest[R](proxyRequest: APIGatewayProxyRequest.WithBody[R]): Either[ApiErrors, UpdateRequest[R]] = {
    proxyRequest.pathParameters.get("id").map(id => {
      try {
        proxyRequest.body match {
          case Some(body) =>
            val withParsedId = UpdateRequest(id = Integer.parseInt(id), body = body)(proxyRequest)
            Right(withParsedId)
          case None =>
            Left(ApiErrors(ApiError.InternalError))
        }
      } catch {
        case e: NumberFormatException =>
          logger.error("`id` path parameter for update request was not an Integer. String path parameters are not currently supported.", e)
          Left(ApiErrors(ApiError.InternalError))
      }
    }).getOrElse({
      logger.error("Missing `id` path parameter for update request. Please add `id` as a path parameter!")
      Left(ApiErrors(ApiError.InternalError))
    })
  }
}