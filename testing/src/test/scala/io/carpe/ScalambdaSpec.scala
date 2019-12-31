package io.carpe

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.ScalambdaSpec.{TestScalambdaInput, TestScalambdaOutput}
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.testing.ScalambdaFixtures
import io.circe.{Decoder, Encoder}
import org.scalatest.FlatSpec

class ScalambdaSpec extends FlatSpec with ScalambdaFixtures[TestScalambdaInput, TestScalambdaOutput] {

  case object TestScalambda extends Scalambda[TestScalambdaInput, TestScalambdaOutput] {
    override def handleRequest(input: TestScalambdaInput, context: Context): TestScalambdaOutput = {
      TestScalambdaOutput(1337)
    }
  }

  override def createHandler: Scalambda[TestScalambdaInput, TestScalambdaOutput] = TestScalambda

  "A basic Function extending Scalambda" should "be invocable" in {
    // send the test request and capture the output
    val actualResponse = sendTestRequest(TestScalambdaInput(message = "hello"))

    // create an expected response object to use for comparison
    val expectedResponse = makeResponse(TestScalambdaOutput(1337))

    // assert the response is as expected
    assert(actualResponse === expectedResponse)
  }
}

object ScalambdaSpec {
  import io.circe.generic.semiauto._

  case class TestScalambdaInput(message: String)
  case class TestScalambdaOutput(value: Int)

  implicit val inputEnc: Encoder[TestScalambdaInput] = deriveEncoder[TestScalambdaInput]
  implicit val inputDec: Decoder[TestScalambdaInput] = deriveDecoder[TestScalambdaInput]
  implicit val outputEnc: Encoder[TestScalambdaOutput] = deriveEncoder[TestScalambdaOutput]
  implicit val outputDec: Decoder[TestScalambdaOutput] = deriveDecoder[TestScalambdaOutput]
}

