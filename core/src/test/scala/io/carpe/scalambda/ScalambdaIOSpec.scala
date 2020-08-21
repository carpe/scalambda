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

  it should "call post processing hook after closing the output stream" in {
    val test = for {
      // create tracker for whether or not we ran the run method
      mainProcessingTracker <- Ref[IO].of(false)
      // create tracker for whether or not we closed the output stream
      outputStreamClosedTracker <- Ref[IO].of(false)

      // create tracker for whether or not we ran the post method
      postProcessingTracker <- Ref[IO].of(false)

      // create modified output stream that updates the tracker when it is closed
      os = new ByteArrayOutputStream() {
        override def close(): Unit = outputStreamClosedTracker.set(true).unsafeRunSync()
      }

      testInstance: ScalambdaIO[Int, Int] = new ScalambdaIO[Int, Int]() {
        override def run(i: Int, context: Context): IO[Int] = for {
          // update tracker
          _ <- mainProcessingTracker.set(true)
        } yield {
          i
        }

        override def post(input: Either[Throwable, Int], output: Either[Throwable, Int], context: Context): IO[Unit] = for {
          // check that the main tracker has already ran
          isRunMethodComplete <- mainProcessingTracker.get
          isOutputStreamClosed <- outputStreamClosedTracker.get
          _ <- IO {
            // check that we have already run post processing
            assert(isRunMethodComplete, "post processing somehow ran before run method")

            // check that the output stream has already been closed by writing to it, which should throw an IOException
            assert(isOutputStreamClosed, "output stream was not closed before the post processing hook")
          }

          // set post processing tracker to true to inform test that we did in fact run post processing
          _ <- postProcessingTracker.set(true)
        } yield ()
      }

      // create test input/output streams
      is = createTestInputStream(1337)

      // invoke the function
      _ <- IO {
        testInstance.handler(is, os, testContext)
      }

      // check post processing after function invocation has completed
      postProcessingRan <- postProcessingTracker.get
    } yield {
      assert(postProcessingRan, "post processing ran successfully")
    }
    test.unsafeRunSync()
  }
}
