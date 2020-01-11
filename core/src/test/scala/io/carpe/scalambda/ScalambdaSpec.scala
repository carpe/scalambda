package io.carpe.scalambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.ScalambdaSpec.{Car, CarValidator, Wheel}
import io.carpe.scalambda.helpers.LambdaTestFixtures
import io.circe.{Decoder, Encoder}
import org.scalatest.flatspec.AnyFlatSpec

class ScalambdaSpec extends AnyFlatSpec with LambdaTestFixtures {
  "Scalambda" should "take in a request and provide a response" in {
    val validCar = Car(
      wheels = Seq(Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40)),
      hp = 1337
    )

    val response = sendTestRequest(validCar)

    assert(response ===
      """{
        | "wheels": [
        |   {"brandName":"Pirelli","width":20,"diameter":40},
        |   {"brandName":"Pirelli","width":20,"diameter":40},
        |   {"brandName":"Pirelli","width":20,"diameter":40},
        |   {"brandName":"Pirelli","width":20,"diameter":40}
        | ],
        | "hp":1337
        |}
        |""".stripMargin
          // remove all whitespace from the expected result
          .replaceAll("\\s", ""))
  }

  it should "produce a response without null values" in {
    val validCar = Car(
      wheels = Seq(Wheel(None, 20, 40), Wheel(None, 20, 40), Wheel(None, 20, 40), Wheel(None, 20, 40)),
      hp = 1337
    )

    val response = sendTestRequest(validCar)

    assert(response ===
      """{
        | "wheels": [
        |   {"width":20,"diameter":40},
        |   {"width":20,"diameter":40},
        |   {"width":20,"diameter":40},
        |   {"width":20,"diameter":40}
        | ],
        | "hp":1337
        |}
        |""".stripMargin
        // remove all whitespace from the expected result
        .replaceAll("\\s", ""))
  }

  /**
   * This is a helper function used to test the lambda function
   * @param body input to the lambda
   * @param encoder implicit encoder for input
   * @tparam I input type
   * @return
   */
  def sendTestRequest[I](body: I)(implicit encoder: Encoder[I]): String = {
    // create output buffer to capture lambda output in
    val output = new ByteArrayOutputStream()

    // serialize the input
    val serializedRequest = encoder(body).noSpaces.stripMargin
    val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

    // invoke the lambda which will write to the output buffer
    new CarValidator().handler(streamFromString(serializedRequest), output, makeContext())

    // return output as string
    output.toString
  }
}

object ScalambdaSpec {

  import io.circe.generic.semiauto._

  case class Car(wheels: Seq[Wheel], hp: Int)

  implicit val carEncoder: Encoder[Car] = deriveEncoder[Car]
  implicit val carDecoder: Decoder[Car] = deriveDecoder[Car]

  case class Wheel(brandName: Option[String], width: Int, diameter: Int)

  implicit val wheelEncoder: Encoder[Wheel] = deriveEncoder[Wheel]
  implicit val wheelDecoder: Decoder[Wheel] = deriveDecoder[Wheel]

  class CarValidator extends Scalambda[Car, Option[Car]] {

    /**
     * Filters Cars, only allowing cars with more than 42 HP.
     *
     * @param input to check horsepower of
     * @param context lambda context
     * @return
     */
    override def handleRequest(input: Car, context: Context): Option[Car] = {
      if (input.hp > 42) {
        Some(input)
      } else {
        None
      }
    }
  }
}
