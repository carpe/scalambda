package io.carpe.scalambda

import java.io.{InputStream, OutputStream}

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.circe
import io.circe.parser.decodeAccumulating
import io.circe.{Decoder, Encoder, Printer}

import scala.collection.immutable
import scala.io.Source

abstract class Scalambda[I, O](implicit val dec: Decoder[I], val enc: Encoder[O])
  extends LazyLogging {

  /**
   * This is the handler that will be called by AWS when executing the Lambda Function.
   *
   * @param is      inputstream for input
   * @param os      outputstream for output
   * @param context aws request context
   */
  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {

    // wrap input stream with resource
    Resource.make(IO.pure(is))(is => IO {
      is.close()
    }).use(inputStream => {
      // read input stream for parsing
      val inputString = Source.fromInputStream(inputStream).mkString
      logger.trace(s"Input to function was: ${inputString}")

      // wrap output stream with resource
      val output = Resource.make(IO.pure(os))(os => IO {
        os.close()
      })

      // attempt to parse input
      decodeAccumulating[I](inputString).fold(
        // if unsuccessful,
        errors => {
          // ... call the defined handler for invalid input
          handleInvalidInput(output, errors)
        },
        // if successful,
        req => {
          // ... run the defined handler function
          val resp = handleRequest(req, context)

          // write the response to the output stream and then close it
          output.use(os => IO {
            val outputString = Scalambda.encode[O](resp)
            os.write(outputString.getBytes)
          })
        }
      )
    }).unsafeRunSync()
  }

  def handleRequest(input: I, context: Context): O


  /**
   * This is the function that Scalambda will call in the event of invalid input.
   *
   * You can override this to modify what is returned when invalid input is provided.
   *
   * @param outputStream to write the response to. The stream will be closed by scalambda
   * @param errors       encountered when parsing the input
   * @return
   */
  protected def handleInvalidInput(outputStream: Resource[IO, OutputStream], errors: NonEmptyList[circe.Error]): IO[Unit] = {
    import cats.implicits._

    for {
      logErrors <- errors.map(err => IO {
        logger.error("Failed to parse input.", err)
      }).sequence
    } yield {
      logger.error("Failed to parse input, see errors in earlier logs for details.")
      val encodedErrors = Scalambda.encode(errors.toList.map(_.getMessage))
      outputStream.use(o => IO {
        o.write(encodedErrors.getBytes)
      })
    }
  }

}

object Scalambda {

  private lazy val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def encode[T](obj: T)(implicit encoder: Encoder[T]): String = {
    import io.circe.syntax._
    printer.print(obj.asJson)
  }
}