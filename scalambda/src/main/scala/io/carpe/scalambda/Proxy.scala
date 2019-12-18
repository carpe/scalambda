package io.carpe.scalambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.circe.{Encoder, Error, parser}

import scala.io.Source

trait Proxy[F] extends Proxy.ProxyEncoder with LazyLogging {

  def handler(inputString: String, context: Context): String

  final def handler[T](is: InputStream, os: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(is).mkString

    logger.debug(s"Lambda Proxy Input: $inputString")

    val outputString = Proxy.checkForWarmer(inputString).fold(
      handler(inputString, context)
    )(
      warmer => "ACK"
    )

    logger.debug(s"Lambda Proxy Output: $outputString")

    os.write(outputString.getBytes)
    os.close()
  }

  def invalidInput(circeError: Error): APIGatewayProxyResponse[F]
}

object Proxy {

  type Response[F, S] = Either[APIGatewayProxyResponse[F], APIGatewayProxyResponse[S]]

  val WARMER_KEY = "X-LAMBDA-WARMER"

  def checkForWarmer(input: String): Option[Unit] = parser.parse(input).toOption.flatMap(
    json => json.hcursor.get[Boolean](WARMER_KEY).getOrElse(false) match {
      case true => Some(())
      case false => None
    }
  )

  trait ProxyEncoder {
    protected def encode[T](obj: APIGatewayProxyResponse[T])(implicit encoder: Encoder[T],
                                                             stringEncoder: Encoder[String]): String = {
      import io.circe.syntax._
      val bodyAsJsonString = obj.body.map(_.asJson.noSpaces)
      obj.copy[String](body = bodyAsJsonString).asJson(APIGatewayProxyResponse.encode[String]).noSpaces
    }
  }
}
