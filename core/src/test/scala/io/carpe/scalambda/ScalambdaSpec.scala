package io.carpe.scalambda

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.effect.ScalambdaIO
import org.scalatest.flatspec.AnyFlatSpec

class ScalambdaSpec extends AnyFlatSpec with ScalambdaFixtures {

  "Scalambda" should "be implementable" in {
    val echo: Scalambda[String, String] = new Scalambda[String, String]() {
      override def handleRequest(input: String, context: Context): String = {
        input
      }
    }

    val pong = echo.handleRequest("ping", testContext)

    assert("ping" === pong)
  }
}
