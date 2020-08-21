package io.carpe.scalambda.testing

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.Scalambda
import io.circe.{Decoder, DecodingFailure, Encoder}

trait ScalambdaFixtures extends LazyLogging {

  protected[testing] val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

  /**
   * This is a helper function used to test the lambda function. It wil invoke the the lambda and return a String made
   * from the JSON output of the Lambda function.
   *
   * It is recommended that you use the [[testRequest()]] below function to test functionality of your lambda function.
   * Only use this function if you need to make assertions about the formatting of the JSON output itself. For instance,
   * if you want to make sure that the output of your function contains no nulls or is properly formatted.
   *
   * @param handler to test
   * @param body input to the lambda
   * @param encoder implicit encoder for input
   * @tparam I input type
   * @return
   */
  def testRequestJson[I](handler: Scalambda[I, _], body: I)(implicit encoder: Encoder[I]): String = {
    // create output buffer to capture lambda output in
    val output = new ByteArrayOutputStream()

    // serialize the input
    val serializedRequest = encoder(body).noSpaces.stripMargin
    val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

    // invoke the lambda which will write to the output buffer
    handler.handler(streamFromString(serializedRequest), output, MockContext.default)

    // return output as string
    output.toString
  }

  /**
   * Sends a test request and deserializes the output so that it can be used in a comparison.
   *
   * @param handler to test
   * @param serializedRequest to send
   * @param decoder for output
   * @param requestContext mock context to use for test request
   * @tparam I input
   * @tparam O output
   * @return the output provided by the handler
   */
  def testRequest[I, O](handler: Scalambda[I, O], serializedRequest: String)(implicit decoder: Decoder[O], requestContext: Context): O = {
    val testOutputStream  = new ByteArrayOutputStream()

    val testInput = streamFromString(serializedRequest)

    handler.handler(testInput, testOutputStream, requestContext)

    val testOutput = testOutputStream.toString

    import io.circe.parser._

    parse(testOutput).fold(
      err =>
        throw err
      , success =>
        success.as[O]

    ) match {
      case Left(value: DecodingFailure) =>
        throw value
      case Right(value) =>
        value
    }
  }
}
