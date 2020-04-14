package io.carpe.scalambda

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.ScalambdaSpec.CarValidator
import io.carpe.scalambda.fixtures.TestModels._
import io.carpe.scalambda.testing.ScalambdaFixtures
import org.scalatest.flatspec.AnyFlatSpec

class ScalambdaSpec extends AnyFlatSpec with ScalambdaFixtures {
  "Scalambda" should "take in a request and provide a response" in {

    val response = testRequestJson(new CarValidator(), validCar)

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
    val response = testRequestJson(new CarValidator(), validCarWithNulls)

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
}

object ScalambdaSpec {

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
