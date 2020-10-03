package io.carpe.scalambda.native

import cats.effect.{ContextShift, IO}
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.native.ScalambdaIO.RequestEvent
import io.carpe.scalambda.native.exceptions.MissingHeaderException
import io.carpe.scalambda.native.views.LambdaError
import io.circe.{Decoder, Encoder, Printer}
import requests.Response

import scala.concurrent.ExecutionContext

abstract class ScalambdaIO[I, O](implicit val decoder: Decoder[I], val encoder: Encoder[O]) extends LazyLogging {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def main(args: Array[String]): Unit = {
    val runtimeApi: String = sys.env("AWS_LAMBDA_RUNTIME_API")

    // The url used to retrieve request events
    val nextEventUrl: String = s"http://$runtimeApi/2018-06-01/runtime/invocation/next"

    // create IO to represent polling the runtime api for a new event to handle
    lazy val pollForEvent: IO[RequestEvent] = for {
      // continuously check for an incoming request event
      r <- {
        // Create endlessly looping request
        lazy val fetchEvent: IO[Response] = IO {
          logger.trace(s"Attempting to fetch event from ${nextEventUrl}")
          requests.get(nextEventUrl)
        }.handleErrorWith {
          // Sometimes the lambda runtime will be left to run for a long time without incoming events. In this event, the
          // lambda runtime api will simply not respond to requests, instead of returning an error.
          case _: requests.TimeoutException =>
            fetchEvent
          case err: Throwable =>
            IO.raiseError(err)
        }

        // return the endlessly looping request
        fetchEvent
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
      eventResponse <- eventBody.fold(IO.raiseError, run).attempt

      // send the result of the event back to the aws lambda service
      _ <- eventResponse.fold(err => {
        val error = LambdaError(errorType = err.getClass.getCanonicalName, errorMessage = err.getMessage)
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
      _ <- IO.shift
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
