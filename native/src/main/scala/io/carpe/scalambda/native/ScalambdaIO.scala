package io.carpe.scalambda.native

import cats.effect.{ContextShift, IO}
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.native.ScalambdaIO.RequestEvent
import io.carpe.scalambda.native.exceptions.MissingHeaderException
import io.carpe.scalambda.native.views.LambdaServiceResponse
import io.circe.{Decoder, Encoder, Printer}

import scala.concurrent.ExecutionContext

abstract class ScalambdaIO[I, O](implicit val decoder: Decoder[I], val encoder: Encoder[O]) extends LazyLogging {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def main(args: Array[String]): Unit = {
    val runtimeApi: String = sys.env("AWS_LAMBDA_RUNTIME_API")

    // The url used to retrieve request events
    val nextEventUrl: String = s"http://$runtimeApi/2018-06-01/runtime/invocation/next"

    lazy val pollForEvent: IO[RequestEvent] = for {
      // check for an incoming request event
      r <- IO {
        requests.get(nextEventUrl)
      }

      // decode inputs from the request event
      requestId <- fetchRequiredHeader(r.headers, "lambda-runtime-aws-request-id")

      // send the response back to the AWS lambda service
    } yield RequestEvent(r.text(), requestId)

    lazy val runLoop: IO[Unit] = for {
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
      serviceResponse: LambdaServiceResponse = eventResponse.fold(err => {
        val serializedError = err.getMessage
        LambdaServiceResponse(statusCode = "500", headers = Map("Content-Type" -> "text/plain"), body = serializedError, isBase64Encoded = false)
      }, result => {
        val serializedResult: String = ScalambdaIO.encode(result)
        LambdaServiceResponse(statusCode = "200", headers = Map("Content-Type" -> "application/json"), body = serializedResult, isBase64Encoded = false)
      })
      _ <- IO {
        // serialize response
        val serializedServiceResponse = ScalambdaIO.encode(serviceResponse)

        // generate url to send the result of the function invocation to
        val responseUrl = s"http://$runtimeApi/2018-06-01/runtime/invocation/${event.requestId}/response"

        // send it
        requests.post(responseUrl, data = serializedServiceResponse)
      }

      // trampoline and repeat the runLoop.
      // this allows us to continue checking for additional requests to process before AWS terminates this instance
      _ <- IO.shift
      _ <- runLoop
    } yield ()
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
   * @param headers to fetch from
   * @param targetHeader  name of the header to fetch
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
   * @param eventRequestBody body of the event
   * @param requestId id of the event
   */
  case class RequestEvent(eventRequestBody: String, requestId: String)

  // printer we will use to print json
  private lazy val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  /**
   * Helper to turn an object into JSON using some implicitly defined encoder.
   *
   * @param obj to encode
   * @param encoder to encode with
   * @tparam T type of the object we are encoding
   * @return the object as JSON
   */
  def encode[T](obj: T)(implicit encoder: Encoder[T]): String = {
    import io.circe.syntax._
    printer.print(obj.asJson)
  }
}
