package io.carpe.scalambda.effect

import java.io.{InputStream, OutputStream}

import cats.effect.{IO, Resource}
import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.Scalambda
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}

import scala.io.Source

abstract class ScalambdaIO[I, O](implicit val decoder: Decoder[I], val encoder: Encoder[O]) extends LazyLogging {

  /**
   * This is the handler that will be called by AWS when executing the Lambda Function.
   *
   * @param is      inputstream for input
   * @param os      outputstream for output
   * @param context aws request context
   */
  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {

    // wrap input stream with resource
    val input = Resource.make(IO.pure(is))(is => IO {
      is.close()
    })

    // wrap output stream with resource
    val output = Resource.make(IO.pure(os))(os => IO {
      os.close()
    })

    val process = for {
      req <- input.use(inputStream => {
        // read input stream for parsing
        val inputString = Source.fromInputStream(inputStream).mkString
        logger.trace(s"Input to function was: ${inputString}")


        // attempt to parse input
        decode[I](inputString).fold(
          // if unsuccessful,
          err => IO.raiseError(err),
          // if successful,
          req => IO.pure(req)
        )
      }).attempt

      // run actual logic inside this function
      result <- req.fold(
        IO.raiseError,
        req => run(req, context)

        // handle errors with defined error handling
      ).attempt

      // write the response to the output stream and then close it
      _ <- result.fold(
        err => error(output, err, context),
        success =>
          output.use(os => IO {
            val outputString = Scalambda.encode[O](success)
            os.write(outputString.getBytes)
          })
      )

      // perform post processing
      _ <- post(req, result, context)

    } yield {
      ()
    }

    process.unsafeRunSync()
  }

  def run(i: I, context: Context): IO[O]


  /**
   * In the event of an error, this function is called, allowing for custom error handling.
   *
   * @param outputStream use this resource to write a response in the event of an error
   * @param err          that was thrown
   * @param context      for the invocation
   * @return
   */
  def error(outputStream: Resource[IO, OutputStream], err: Throwable, context: Context): IO[Unit] = {
    outputStream.use(os => IO {
      logger.error("Function invocation failed due to unhandled exception being thrown", err)
      os.write(err.getMessage.getBytes)
    })
  }

  /**
   * A hook that you can provide that will run after the lambda provides a response to the client. This is useful if you
   * want to calculate or log any metrics without impacting the Lambda's response time.
   *
   * @param input   that was originally provided in the invocation
   * @param output  of the function
   * @param context for the invocation
   * @return
   */
  def post(input: Either[Throwable, I], output: Either[Throwable, O], context: Context): IO[Unit] = IO.unit

}
