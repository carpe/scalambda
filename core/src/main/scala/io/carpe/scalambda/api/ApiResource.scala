package io.carpe.scalambda.api

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.api.ApiResource.defaultResponseHeaders
import io.carpe.scalambda.api.conf.{ApiBootstrap, ScalambdaApi}
import io.carpe.scalambda.api.delete.DeleteRequest
import io.carpe.scalambda.api.index.IndexRequest
import io.carpe.scalambda.api.show.ShowRequest
import io.carpe.scalambda.api.update.UpdateRequest
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError}
import io.circe.{Decoder, Encoder, Json}

// Used to implicitly invoke the application bootstrapper to convert each Lambda function context into request context
import scala.language.implicitConversions

/**
 * An [[ApiResource]] allows for the definition of simple IOMonad based request handling.
 *
 * This removes much of the boilerplate for handling specific requests, as well as allow you to write logic for handling
 * requests without having to worry about side-effects.
 *
 * @tparam C context type, this can be used to map the lambda context into something more specific to your application.
 *           It also allows for easier testing, by allowing the context to be captured by test helpers so that mocks can
 *           be injected.
 * @tparam I input type, which is set on a per request type basis
 * @tparam RI the specific type of api gateway proxy request this function accepts.
 * @tparam O output type, which is set on a per request type basis
 */
sealed abstract class ApiResource[C <: ScalambdaApi, +I, RI <: APIGatewayProxyRequest[I], O](implicit val bootstrap: ApiBootstrap[C], val inputDecoder: Decoder[RI])
  extends Scalambda[RI, APIGatewayProxyResponse[O]] {
  implicit def init(context: Context): C = bootstrap(context)

  final override def handleRequest(input: RI, context: Context): APIGatewayProxyResponse[O] = {
    val c: C = init(context)
    handleApiRequest(input)(c)
  }

  def handleApiRequest(input: RI)(implicit c: C): APIGatewayProxyResponse[O]

  protected[scalambda] def handleError(err: Throwable): APIGatewayProxyResponse[O] = {
    err match {
      case apiError: ApiError =>
        APIGatewayProxyResponse.WithError(defaultResponseHeaders, apiError)
      case _ =>
        logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
        APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiError.InternalError)
    }
  }
}

object ApiResource {

  // type aliases used to simplify lambda definitions
  type WithoutBody[C <: ScalambdaApi, O] = ApiResource[C, Nothing, APIGatewayProxyRequest.WithoutBody, O]
  type WithBody[C <: ScalambdaApi, O] = ApiResource[C, O, APIGatewayProxyRequest.WithBody[O], O]
  type WithoutBodyOrResponse[C <: ScalambdaApi] = ApiResource[C, Nothing, APIGatewayProxyRequest.WithoutBody, Nothing]

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
  abstract class Create[C <: ScalambdaApi, R](implicit val applicationBootstrap: ApiBootstrap[C], val decoder: Decoder[R], val encoder: Encoder[R]) extends ApiResource.WithBody[C, R] {

    override def handleApiRequest(input: APIGatewayProxyRequest.WithBody[R])(implicit context: C): APIGatewayProxyResponse[R] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result: Either[Throwable, R] = for {
        requestBody <- input.body match {
                        case Some(value) =>
                          Right(value)
                        case None =>
                          Left(ApiError.InternalError)
                      }
        createRecord <- {
          create(requestBody)(context).attempt.unsafeRunSync()
        }
      } yield {
        createRecord
      }

      // return a response based on the result of the record creation
      result.fold(handleError, success => APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, success))
    }

    /**
     * Create a record
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to create the record
     */
    def create(input: R)(implicit api: C): IO[R]
  }

  /**
   * An [[ApiResource]] used to handle GET requests for a single record by ID. Example: "/cars/42:GET"
   *
   * @param encoder for the record
   * @tparam R type of record
   */
  abstract class Show[C <: ScalambdaApi, R](implicit val applicationBootstrap: ApiBootstrap[C], val encoder: Encoder[R])
      extends ApiResource.WithoutBody[C, R] {

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithoutBody
    )(implicit context: C): APIGatewayProxyResponse[R] = {
      ShowRequest.fromProxyRequest(input) match {
        case Left(err) =>
          APIGatewayProxyResponse.WithError(defaultResponseHeaders, err)

        case Right(showInput) =>
          val records = show(showInput)(context)

          records.attempt.unsafeRunSync() match {
            case Left(err) =>
              handleError(err)

            case Right(success) =>
              success match {
                case Some(record) =>
                  APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, record)
                case None =>
                  APIGatewayProxyResponse.WithError(
                    defaultResponseHeaders,
                    ApiError.NotFoundError(s"Could not find record with id: ${showInput.id}.")
                  )
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
    def show(input: ShowRequest)(implicit api: C): IO[Option[R]]
  }

  /**
   * An [[ApiResource]] used to handle GET requests for multiple records. Example: "/cars:GET"
   *
   * @param encoder for records
   * @tparam R type of record
   */
  abstract class Index[C <: ScalambdaApi, R](
    implicit val applicationBootstrap: ApiBootstrap[C],
    val encoder: Encoder[R]
  ) extends ApiResource.WithoutBody[C, R] {

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithoutBody
    )(implicit context: C): APIGatewayProxyResponse[R] = {

      val records = index(IndexRequest.fromProxyRequest(input))(context)

      records.attempt.unsafeRunSync() match {
        case Left(err) =>
          handleError(err)

        case Right(success) =>
          APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, success)

      }

    }

    /**
     * Get multiple records.
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to retrieving the records
     */
    def index(input: IndexRequest)(implicit api: C): IO[R]
  }

  /**
   * An [[ApiResource]] used to handle PUT requests for updating records. Example: "/cars:PUT"
   *
   * @param decoder for record
   * @param encoder for record
   * @tparam R type of record
   */
  abstract class Update[C <: ScalambdaApi, R](
    implicit val applicationBootstrap: ApiBootstrap[C],
    val decoder: Decoder[R],
    val encoder: Encoder[R]
  ) extends ApiResource.WithBody[C, R] {

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithBody[R]
    )(implicit context: C): APIGatewayProxyResponse[R] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result = for {
        updateRequest <- UpdateRequest.fromProxyRequest(input)
        updateRecord <- update(updateRequest)(context).attempt.unsafeRunSync()
      } yield {
        updateRecord
      }

      // return a response based on the result of the update
      result.fold(handleError, success => APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, success))
    }

    /**
     * Update a record
     *
     * @param input for request
     * @return an IO Monad that wraps logic for attempting to update the record
     */
    def update(input: UpdateRequest[R])(implicit api: C): IO[R]
  }

  /**
   * An [[ApiResource]] used to handle DELETE requests for deleting records. Example: "/cars/2:DELETE"
   */
  abstract class Delete[C <: ScalambdaApi](implicit val applicationBootstrap: ApiBootstrap[C])
      extends ApiResource.WithoutBodyOrResponse[C] {

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithoutBody
    )(implicit context: C): APIGatewayProxyResponse[Nothing] = {
      // try to parse a record from the input and then process it using the supplied implementation
      val result = for {
        requestBody <- DeleteRequest.fromProxyRequest(input)
        deletion <- delete(requestBody)(context).attempt.unsafeRunSync()
      } yield {
        deletion
      }

      // return a response based on the result of the record deletion
      result.fold(handleError, _ => APIGatewayProxyResponse.Empty(204, defaultResponseHeaders))
    }

    /**
     * Delete a record
     *
     * @return an IO Monad that wraps logic for attempting to delete the record
     */
    def delete(input: DeleteRequest)(implicit api: C): IO[Unit]
  }


  /**
   * If your application's endpoint does not fit a RESTful method like those above, you can use this more generic
   * [[Resource]].
   *
   * @param applicationBootstrap bootstrap for context
   * @param decoder for input body
   * @param encoder for response body
   * @tparam C context type, this can be used to map the lambda context into something more specific to your application.
   *           It also allows for easier testing, by allowing the context to be captured by test helpers so that mocks can
   *           be injected.
   * @tparam I input type, which is set on a per request type basis
   * @tparam O output type, which is set on a per request type basis
   */
  abstract class Resource[C <: ScalambdaApi, I, O]( implicit val applicationBootstrap: ApiBootstrap[C],
                                                    val decoder: Decoder[I],
                                                    val encoder: Encoder[O]
                                                  ) extends ApiResource[C, I, APIGatewayProxyRequest.WithBody[I], O] {

    override def handleApiRequest(input: APIGatewayProxyRequest.WithBody[I])(implicit c: C): APIGatewayProxyResponse[O] = {
      request(input).attempt.unsafeRunSync().fold(
        // handle the failure of the IO
        handleError,
        // wrap successful result in a proxy response object so that API Gateway can respond with it
        o => APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, o)
      )
    }

    def request(input: APIGatewayProxyRequest[I])(implicit api: C): IO[O]
  }
}
