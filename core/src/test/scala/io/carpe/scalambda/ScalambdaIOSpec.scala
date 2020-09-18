package io.carpe.scalambda

import java.io.{ByteArrayOutputStream, IOException, OutputStream}

import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.effect.ScalambdaIO
import org.scalatest.flatspec.AnyFlatSpec

class ScalambdaIOSpec extends AnyFlatSpec with ScalambdaFixtures {

  "ScalambdaIO" should "handle requests" in {
    val testInstance: ScalambdaIO[Int, Int] = new ScalambdaIO[Int, Int]() {
      override def run(i: Int, context: Context): IO[Int] = IO {
        i + 41
      }
    }

    // this will be the input to our test function
    val testInput = {
      1
    }

    // create test input/output streams
    val is = createTestInputStream(testInput)
    val os = createTestOutputStream()

    testInstance.handler(is, os, testContext)

    // get contents from output stream after invocation
    val outputContents = os.toString

    assert(outputContents == "42")
  }

  it should "call error handler when it encounters an error" in {
    val expectedErrorMessage = "whoops, man got hot due to lack of quick maths"

    val testInstance: ScalambdaIO[Int, Int] = new ScalambdaIO[Int, Int]() {
      override def run(i: Int, context: Context): IO[Int] = IO {
        // divide by 0 should throw exception
        i / 0
      }

      override def error(outputStream: Resource[IO, OutputStream], err: Throwable, context: Context): IO[Unit] = {
        // when the run block above triggers an exception, this function should be called and write the message
        // below to the output stream
        outputStream.use(os => IO {
          os.write(expectedErrorMessage.getBytes)
        })
      }
    }

    // this will be the input to our test function
    val testInput = {
      1
    }

    // create test input/output streams
    val is = createTestInputStream(testInput)
    val os = createTestOutputStream()

    testInstance.handler(is, os, testContext)

    // get contents from output stream after invocation
    val outputContents = os.toString

    assert(outputContents == expectedErrorMessage)
  }
}
