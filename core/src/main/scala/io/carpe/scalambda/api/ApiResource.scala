package io.carpe.scalambda.api

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.api.ApiResource.ApiConfiguration
import io.carpe.scalambda.api.conf.ApiContext
import io.carpe.scalambda.api.delete.DeleteRequest
import io.carpe.scalambda.api.index.{IndexRequest, IndexResponse}
import io.carpe.scalambda.api.show.ShowRequest
import io.carpe.scalambda.api.update.UpdateRequest
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
 * @tparam I input type, which is set on a per request type basis
 * @tparam O output type, which is set on a per request type basis
 */
sealed abstract class ApiResource[+I, R <: APIGatewayProxyRequest[I], O](implicit val inputDecoder: Decoder[R], val outputEncoder: Encoder[O], val configuration: ApiConfiguration)
  extends Scalambda[R, APIGatewayProxyResponse[O]]

object ApiResource {

  // type aliases used to simplify lambda definitions
  type WithoutBody[O] = ApiResource[Nothing, APIGatewayProxyRequest.WithoutBody, O]
  type WithBody[O] = ApiResource[O, APIGatewayProxyRequest.WithBody[O], O]
  type WithoutBodyOrResponse = ApiResource[Nothing, APIGatewayProxyRequest.WithoutBody, None.type]

  type ApiConfiguration = Context => ApiContext
  lazy val defaultApiConfiguration: ApiConfiguration = ApiContext

  /**
   * Default response headers.
   *
   * TODO: Move this setting to a central configuration object so it can be modified on a per-application basis.
   */
  lazy val defaultResponseHeaders = Map("Access-Control-Allow-Origin" -> "*")

  /**
   * An [[ApiResource]] used to handle POST requests for creating records. Example: "/cars:POST"
   *
   * @param decoder for record
   * @param encoder for record
   * @tparam R type of record
   */
  abstract class Create[R](implicit val decoder: Decoder[R], val encoder: Encoder[R], val conf: ApiConfiguration = defaultApiConfiguration) extends ApiResource.WithBody[R] {
    override def handleRequest(input: APIGatewayProxyRequest.WithBody[R], context: Context): APIGatewayProxyResponse[R] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result = for {
        requestBody <- input.body match {
          case Some(value) =>
            Right(value)
          case None =>
            Left(ApiError.InternalError)
        }
        createRecord <- create(requestBody)(conf(context)).attempt.unsafeRunSync()
      } yield {
        createRecord
      }

      // return a response based on the result of the record creation
      result match {
        case Left(err) =>
          err match {
            case apiError: ApiError =>
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
            case _ =>
              logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
          }

        case Right(success) =>
          APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, success)
      }
    }

    /**
     * Create a record
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to create the record
     */
    def create(input: R)(implicit context: ApiContext): IO[R]
  }

  /**
   * An [[ApiResource]] used to handle GET requests for a single record by ID. Example: "/cars/42:GET"
   *
   * @param encoder for the record
   * @tparam R type of record
   */
  abstract class Show[R](implicit val encoder: Encoder[R], val conf: ApiConfiguration = defaultApiConfiguration) extends ApiResource.WithoutBody[R] {
    override def handleRequest(input: APIGatewayProxyRequest.WithoutBody, context: Context): APIGatewayProxyResponse[R] = {
      ShowRequest.fromProxyRequest(input) match {
        case Left(err) =>
          APIGatewayProxyResponse.WithError(defaultResponseHeaders, err)

        case Right(showInput) =>
          val records = show(showInput)(conf(context))

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
    def show(input: ShowRequest)(implicit context: ApiContext): IO[Option[R]]
  }

  /**
   * An [[ApiResource]] used to handle GET requests for multiple records. Example: "/cars:GET"
   *
   * @param encoder for records
   * @tparam R type of record
   */
  abstract class Index[R](implicit val encoder: Encoder[R], val conf: ApiConfiguration = defaultApiConfiguration) extends ApiResource.WithoutBody[IndexResponse[R]] {
    override def handleRequest(input: APIGatewayProxyRequest.WithoutBody, context: Context): APIGatewayProxyResponse[IndexResponse[R]] = {
      val records = index(IndexRequest.fromProxyRequest(input))(conf(context))

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
    def index(input: IndexRequest)(implicit context: ApiContext): IO[List[R]]
  }

  /**
   * An [[ApiResource]] used to handle PUT requests for updating records. Example: "/cars:PUT"
   *
   * @param decoder for record
   * @param encoder for record
   * @tparam R type of record
   */
  abstract class Update[R](implicit val decoder: Decoder[R], val encoder: Encoder[R], val conf: ApiConfiguration = defaultApiConfiguration) extends ApiResource.WithBody[R] {
    override def handleRequest(input: APIGatewayProxyRequest.WithBody[R], context: Context): APIGatewayProxyResponse[R] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result = for {
        updateRequest <- UpdateRequest.fromProxyRequest(input)
        updateRecord <- update(updateRequest)(conf(context)).attempt.unsafeRunSync()
      } yield {
        updateRecord
      }

      // return a response based on the result of the record creation
      result match {
        case Left(err) =>
          err match {
            case apiError: ApiError =>
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
            case _ =>
              logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
          }

        case Right(success) =>
          APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, success)
      }
    }

    /**
     * Update a record
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to update the record
     */
    def update(input: UpdateRequest[R])(implicit context: ApiContext): IO[R]
  }

  /**
   * An [[ApiResource]] used to handle DELETE requests for deleting records. Example: "/cars/2:DELETE"
   */
  abstract class Delete(implicit val conf: ApiConfiguration = defaultApiConfiguration) extends ApiResource.WithoutBodyOrResponse {
    override def handleRequest(input: APIGatewayProxyRequest.WithoutBody, context: Context): APIGatewayProxyResponse[None.type] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result = for {
        requestBody <- DeleteRequest.fromProxyRequest(input)
        createRecord <- delete(requestBody)(conf(context)).attempt.unsafeRunSync()
      } yield {
        createRecord
      }

      // return a response based on the result of the record creation
      result match {
        case Left(err) =>
          err match {
            case apiError: ApiError =>
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
            case _ =>
              logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
              APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
          }

        case Right(_) =>
          APIGatewayProxyResponse.WithBody(204, defaultResponseHeaders, None)
      }
    }

    /**
     * Delete a record
     *
     * @return an IO Monad that wraps logic for attempting to delete the record
     */
    def delete(input: DeleteRequest)(implicit context: ApiContext): IO[Unit]
  }


}
