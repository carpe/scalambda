package io.carpe.scalambda.native

import cats.data.NonEmptyList
import cats.effect.{IO, Spawn}
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.native.ScalambdaIO.RequestEvent
import io.carpe.scalambda.native.exceptions.MissingHeaderException
import io.carpe.scalambda.native.views.LambdaError
import io.circe
import io.circe.{Decoder, Encoder, Printer}

abstract class ScalambdaIO[I, O](implicit val decoder: Decoder[I], val encoder: Encoder[O]) extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val runtimeApi: String = sys.env("AWS_LAMBDA_RUNTIME_API")

    // The url used to retrieve request events
    val nextEventUrl: String = s"http://$runtimeApi/2018-06-01/runtime/invocation/next"

    // create IO to represent checking the runtime api for a new event to handle
    lazy val pollForEvent: IO[RequestEvent] = for {
      // check for an incoming request event
      r <- IO {
        logger.trace(s"Attempting to fetch event from ${nextEventUrl}")
        // Make request (without a timeout as prescribed by the AWS Custom Lambda Runtime documentation). This is due
        // to the possibility of the runtime being frozen between lambda function invocations. Continuously polling also
        // seems to cause issues with AWS's internal lambda state management service, so this definitely seems to be
        // the best way of fetching the event.
        requests.get(nextEventUrl, connectTimeout = 0, readTimeout = 0)
      }

      // decode inputs from the request event
      requestId <- fetchRequiredHeader(r.headers, "lambda-runtime-aws-request-id")

      // send the response back to the AWS lambda service
    } yield RequestEvent(r.text(), requestId)

    lazy val program: IO[Unit] = for {
      // poll the AWS Lambda service for a request and handle it if one is returned
      event <- pollForEvent.handleErrorWith(err => {
        logger.error("A failure occurred that prevented the Lambda from being able to poll for an event. As such, there is no available request id for this failure", err)
        IO.raiseError(err)
      })

      // decode the event
      eventBody <- decodeInput(event.eventRequestBody).attempt

      // execute the function's defined logic
      eventResponse <- eventBody.fold({
        case circeError: circe.Error =>
          // pass circe error to handle invalid input function
          val errorList = NonEmptyList.one(circeError)
          handleInvalidInput(errorList)
        case circeErrors: circe.Errors =>
          // pass circe error to handle invalid input function
          handleInvalidInput(circeErrors.errors)
        case other: Throwable =>
          // other exceptions are raised so that they can be sent to lambda service later on
          IO.raiseError(other)
      }, input => {
        // use decoded input to run the lambda function
        run(input)
      }).attempt

      // send the result of the event back to the aws lambda service
      _ <- eventResponse.fold(err => {
        // "Class::getCanonicalName" uses reflection, which may or may not be available at runtime depending on the
        // application's configuration. So as a fallback option, we use getName. Should that fail, we use a static
        // String of "runtimeexception".
        val errorType = Option(err.getClass.getCanonicalName)
          .orElse(Option(err.getClass.getName))
          .getOrElse("runtimeexception")

        // use the error type and the error message to construct a LambdaError view, then encode it
        val error = LambdaError(errorType = errorType, errorMessage = err.getMessage)
        val serializedError = ScalambdaIO.encode(error)

        // generate url to send the error to
        val responseUrl = s"http://$runtimeApi/2018-06-01/runtime/invocation/${event.requestId}/error"

        // send it
        IO {
          logger.error(s"Unhandled exception was thrown. An attempt will be made to report it to the lambda service at ${responseUrl}", err)
          requests.post(responseUrl, data = serializedError)
        }
      }, result => {
        val serializedResult: String = ScalambdaIO.encode(result)

        // generate url to send the result of the function invocation to
        val responseUrl = s"http://$runtimeApi/2018-06-01/runtime/invocation/${event.requestId}/response"

        // send it
        IO {
          requests.post(responseUrl, data = serializedResult)
        }
      })

      // trampoline and repeat the program endlessly
      // this allows us to continue checking for additional requests to process before AWS terminates this instance
      _ <- Spawn[IO].cede
      _ <- program
    } yield ()

    // start server
    program.unsafeRunSync()
  }

  /**
   * Decode input from the event request using the provided decoder.
   *
   * @param input to decode
   * @return
   */
  private def decodeInput(input: String): IO[I] = {
    import io.circe.jawn.decode

    // attempt to parse input
    decode[I](input).fold(
      // if unsuccessful,
      err => IO.raiseError(err),
      // if successful,
      req => IO.pure(req)
    )
  }


  /**
   * Helper function that is used to fetch headers from event requests
   *
   * @param headers      to fetch from
   * @param targetHeader name of the header to fetch
   * @return
   */
  private def fetchRequiredHeader(headers: Map[String, Seq[String]], targetHeader: String): IO[String] = {
    headers.get(targetHeader).flatMap(_.headOption).fold[IO[String]]({
      logger.error(s"An event request didn't include the required `$targetHeader` header. The full list of provided headers was ${headers.keySet.mkString(", ")}")
      IO.raiseError(MissingHeaderException(targetHeader))
    })(header => IO.pure(header))
  }

  /**
   * This is the function that Scalambda will call in the event of invalid input.
   *
   * You can override this to modify what is returned when invalid input is provided.
   *
   * @param errors       encountered when parsing the input
   * @return
   */
  protected def handleInvalidInput(errors: NonEmptyList[circe.Error]): IO[O] = {
    import cats.implicits._

    errors.map(err => IO {
      logger.error("Failed to parse input.", err)
    }).sequence

    for {
      // log errors
      logErrors <- errors.map(err => IO {
        logger.error("Failed to parse input.", err)
      }).sequence
      // throw exception so that it is sent to lambda service
      throwError <- IO.raiseError[O](errors.head)
    } yield throwError
  }


  /**
   * Called each time the Lambda is invoked. All work your Lambda performs should be performed inside the IO returned
   * by this function.
   *
   * @param input that was decoded from the event source
   * @return
   */
  def run(input: I): IO[O]
}

object ScalambdaIO {

  /**
   * Helper class for wrapping a single request event from the AWS Service
   *
   * @param eventRequestBody body of the event
   * @param requestId        id of the event
   */
  case class RequestEvent(eventRequestBody: String, requestId: String)

  // printer we will use to print/send responses
  private val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  /**
   * Helper to turn an object into JSON using some implicitly defined encoder.
   *
   * @param obj     to encode
   * @param encoder to encode with
   * @tparam T type of the object we are encoding
   * @return the object as JSON
   */
  def encode[T](obj: T)(implicit encoder: Encoder[T]): String = {
    import io.circe.syntax._
    printer.print(obj.asJson)
  }
}
