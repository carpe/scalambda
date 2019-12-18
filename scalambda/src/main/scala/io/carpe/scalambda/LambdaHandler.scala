package io.carpe.scalambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Errors.InputError
import io.circe
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}

import scala.io.Source

abstract class LambdaHandler[R](implicit val dec: Decoder[R], val enc: Encoder[R]) {

  def invalidInput(circeError: circe.Error): Errors =
    InputError(circeError.getMessage)

  def encode[T](obj: T)(implicit encoder: Encoder[T]): String = {
    import io.circe.syntax._
    obj.asJson.noSpaces
  }

  def handleRequest(r: R, context: Context): R

  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(is).mkString

    val outputString = Proxy.checkForWarmer(inputString).fold({

      val input = decode[R](inputString)

      input.fold(
        error => encode[Errors](invalidInput(error)),
        req => {
          val resp = handleRequest(req, context)

          encode[R](resp)
        }
      )
    })(
      _ => "ACK"
    )

    os.write(outputString.getBytes)
    os.close()
  }
}

