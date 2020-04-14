package io.carpe.scalambda.api

import cats.data.Chain
import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.api.ApiResource.defaultResponseHeaders
import io.carpe.scalambda.api.conf.{ApiBootstrap, ScalambdaApi}
import io.carpe.scalambda.api.delete.DeleteRequest
import io.carpe.scalambda.api.index.IndexRequest
import io.carpe.scalambda.api.show.ShowRequest
import io.carpe.scalambda.api.update.UpdateRequest
import io.carpe.scalambda.api.validation.{Validation, Validator}
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.ApiError.InputError
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError, ApiErrors}
import io.circe
import io.circe.{Decoder, Encoder}

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
sealed abstract class ApiResource[C <: ScalambdaApi, +I, RI <: APIGatewayProxyRequest[I], O](
  implicit val bootstrap: ApiBootstrap[C],
  val inputDecoder: Decoder[RI]
) extends Scalambda[RI, APIGatewayProxyResponse[O]] {
  implicit def init(context: Context): C = bootstrap(context)

  final override def handleRequest(input: RI, context: Context): APIGatewayProxyResponse[O] = {
    val c: C = init(context)
    handleApiRequest(input)(c)
      .fold(errors => APIGatewayProxyResponse.WithError(defaultResponseHeaders, errors), identity)
  }

  def handleApiRequest(input: RI)(implicit c: C): Either[ApiErrors, APIGatewayProxyResponse[O]]

  final protected[scalambda] def performIO[A](io: IO[A]): Either[ApiErrors, A] = {
    io.attempt
      .unsafeRunSync()
      .fold(e => Left(handleError(e)), Right(_))
  }

  final protected[scalambda] def handleError(err: Throwable): ApiErrors = {
    err match {
      case apiError: ApiError =>
        ApiErrors(apiError)
      case _ =>
        logger.error("Request could not be handled due to an unexpected exception being thrown.", err)
        ApiErrors(ApiError.InternalError)
    }
  }

  /**
   * This is the function that Scalambda will call in the event of invalid input.
   *
   * You can override this to modify what is returned when invalid input is provided.
   *
   * @param decodeError produced by circe
   * @return
   */
  override protected def handleInvalidInput(decodeError: circe.Error): String = {
    val errorResponse = APIGatewayProxyResponse.WithError(defaultResponseHeaders, ApiErrors(
      InputError(decodeError.getMessage)
    ))

    Scalambda.encode[APIGatewayProxyResponse[O]](errorResponse)
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
  abstract class Create[C <: ScalambdaApi, R](
    implicit val applicationBootstrap: ApiBootstrap[C],
    val decoder: Decoder[R],
    val encoder: Encoder[R]
  ) extends ApiResource.WithBody[C, R] {

    lazy val validations: Seq[Validation[C, R]] = Nil
    private lazy val validator: Validator[C, R] = Validator(Chain.fromSeq(validations))

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithBody[R]
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[R]] = {
      // try to parse a record from the input and then process it using the supplied implementation
      for {
        requestBody <- input.body match {
                        case Some(value) =>
                          Right(value)
                        case None =>
                          Left(ApiErrors(ApiError.InternalError))
                      }

        validatedBody <- {
          validator.validate(requestBody)
        }

        handlerResult <- {
          performIO(create(validatedBody)(api))
        }

      } yield {
        // return a response based on the result of the record creation
        APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, handlerResult)
      }
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
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[R]] = {
      for {
        showRequest <- ShowRequest.fromProxyRequest(input)

        maybeRecord <- performIO(show(showRequest)(api))

        showResult <- maybeRecord match {
                       case Some(record) =>
                         Right(APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, record))
                       case None =>
                         Left(ApiErrors(ApiError.NotFoundError(s"Could not find record with id: ${showRequest.id}.")))
                     }
      } yield {
        showResult
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
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[R]] = {
      for {
        records <- performIO(index(IndexRequest.fromProxyRequest(input))(api))
      } yield {
        APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, records)
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

    lazy val validations: Seq[Validation[C, R]] = Nil
    private lazy val validator: Validator[C, R] = Validator(Chain.fromSeq(validations))

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithBody[R]
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[R]] = {

      for {
        // try to parse a record from the input and then process it using the supplied implementation
        updateRequest <- UpdateRequest.fromProxyRequest(input)

        // validate the body of the input
        validUpdateBody <- validator.validate(updateRequest.body)

        // merge result of validations into initial update request
        validUpdateRequest = updateRequest.copy(body = validUpdateBody)(input)

        updateRecord <- performIO(update(validUpdateRequest)(api))
      } yield {
        // return a response based on the result of the update
        APIGatewayProxyResponse.WithBody(201, defaultResponseHeaders, updateRecord)
      }
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
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[Nothing]] = {
      // try to parse a record from the input and then process it using the supplied implementation
      for {
        requestBody <- DeleteRequest.fromProxyRequest(input)
        successfulDelete <- performIO(delete(requestBody)(api))
      } yield {
        // return a response based on the result of the record deletion
        APIGatewayProxyResponse.Empty(204, defaultResponseHeaders)
      }
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
  abstract class Resource[C <: ScalambdaApi, I, O](
    implicit val applicationBootstrap: ApiBootstrap[C],
    val decoder: Decoder[I],
    val encoder: Encoder[O]
  ) extends ApiResource[C, I, APIGatewayProxyRequest.WithBody[I], O] {

    override def handleApiRequest(
      input: APIGatewayProxyRequest.WithBody[I]
    )(implicit api: C): Either[ApiErrors, APIGatewayProxyResponse[O]] = {
      for {
        handlerResult <- performIO(request(input)(api))
      } yield {
        // wrap successful result in a proxy response object so that API Gateway can respond with it
        APIGatewayProxyResponse.WithBody(200, defaultResponseHeaders, handlerResult)
      }
    }

    def request(input: APIGatewayProxyRequest[I])(implicit api: C): IO[O]
  }
}
