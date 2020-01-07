package io.carpe.scalambda.api

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.{ApiScalambda, Scalambda}
import io.carpe.scalambda.api.index.{IndexRequest, IndexResponse}
import io.carpe.scalambda.api.show.ShowRequest
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError}
import io.circe.{Decoder, Encoder}

/**
 * An [[ApiResource]] allows for the definition of simple IOMonad based request handling.
 *
 * This removes much of the boilerplate for handling specific requests, as well as allow you to write logic for handling
 * requests without having to worry about side-effects.
 *
 * @param outputEncoder encoder for output
 * @tparam O output type, which is set on a per request type basis
 */
sealed abstract class ApiResource[O](implicit val outputEncoder: Encoder[O])
  extends Scalambda[APIGatewayProxyRequest.WithoutBody, APIGatewayProxyResponse[O]]

object ApiResource {

  /**
   * Default response headers.
   *
   * TODO: Move this setting to a central configuration object so it can be modified on a per-application basis.
   */
  lazy val defaultResponseHeaders = Map("Access-Control-Allow-Origin" -> "*")


  /**
   * An [[ApiResource]] used to handle GET requests for a single record by ID. Example: "/cars/42"
   *
   * @param encoder for the record
   * @tparam R type of record
   */
  abstract class Show[R](implicit val encoder: Encoder[R]) extends ApiResource[R] {
    override def handleRequest(input: APIGatewayProxyRequest.WithoutBody, context: Context): APIGatewayProxyResponse[R] = {
      ShowRequest.fromProxyRequest(input) match {
        case Left(err) =>
          APIGatewayProxyResponse.WithError(defaultResponseHeaders, err)

        case Right(showInput) =>
          val records = show(showInput)

          records.attempt.unsafeRunSync() match {
            case Left(err) =>
              err match {
                case apiError: ApiError =>
                  APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
                case _ =>
                  logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
                  APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
              }

            case Right(success) =>
              success match {
                case Some(record) =>
                  APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, record)
                case None =>
                  APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.NotFoundError(s"Could not find record with id: ${showInput.id}."))
              }
          }
      }
    }

    /**
     * Get a single record by ID
     *
     * @param input for request
     * @return an IOMonad that wraps the logic for attempting to retrieve the single record, if it exists
     */
    def show(input: ShowRequest): IO[Option[R]]
  }

  /**
   * An [[ApiResource]] used to handle GET requests for multiple records. Example: "/cars"
   *
   * @param encoder for records
   * @tparam R type of record
   */
  abstract class Index[R](implicit val encoder: Encoder[R]) extends ApiResource[IndexResponse[R]] {
    override def handleRequest(input: APIGatewayProxyRequest.WithoutBody, context: Context): APIGatewayProxyResponse[IndexResponse[R]] = {
      val records = index(IndexRequest.fromProxyRequest(input))

      records.attempt.unsafeRunSync() match {
        case Left(err) =>
          err match {
            case apiError: ApiError =>
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
            case _ =>
              logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
          }

        case Right(success) =>
          val serializableOutput = IndexResponse(success)
          APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, serializableOutput)

      }


    }

    /**
     * Get multiple records.
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to retrieving the records
     */
    def index(input: IndexRequest): IO[List[R]]
  }
}
