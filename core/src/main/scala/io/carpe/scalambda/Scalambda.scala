package io.carpe.scalambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError}
import io.carpe.scalambda.response.ApiError.InputError
import io.circe
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, Printer}

import scala.io.Source

abstract class Scalambda[I, O](implicit val dec: Decoder[I], val enc: Encoder[O])
  extends LazyLogging {

  /**
   * This is the handler that will be called by AWS when executing the Lambda Function.
   *
   * @param is inputstream for input
   * @param os outputstream for output
   * @param context aws request context
   */
  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {
    // read the input
    val inputString = Source.fromInputStream(is).mkString

    logger.trace(s"Function invoked with: ${inputString}")

    // check if the request is a warmer (aka no-op) request
    val outputString = Scalambda.checkForWarmer(inputString).fold({
      // attempt to parse input
      decode[I](inputString).fold(
        // if unsuccessful, render an error as the body
        error =>
          {
            logger.error("Failed to decode input:", error)
            handleInvalidInput(error)
          },
        // if successful, run the defined handler function
        req => {
          val resp = handleRequest(req, context)
          Scalambda.encode[O](resp)
        }
      )
    })(
      // Respond to warmer request
      _ => "ACK"
    )

    // write the response to the output stream and then close it
    os.write(outputString.getBytes)
    os.close()

    logger.trace(s"Output was: ${outputString}")
  }

  def handleRequest(input: I, context: Context): O


  /**
   * This is the function that Scalambda will call in the event of invalid input.
   *
   * You can override this to modify what is returned when invalid input is provided.
   *
   * @param decodeError produced by circe
   * @return
   */
  protected def handleInvalidInput(decodeError: circe.Error): String = decodeError.getMessage

}

object Scalambda {

  lazy val LAMBDA_WARMER_CODE = "STAY_WARM"

  def checkForWarmer(requestInput: String): Option[Unit] = {
    if (requestInput.equals(Scalambda.LAMBDA_WARMER_CODE)) {
      Some(())
    } else {
      None
    }
  }

  private lazy val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def encode[T](obj: T)(implicit encoder: Encoder[T]): String = {
    import io.circe.syntax._
    printer.print(obj.asJson)
  }
}