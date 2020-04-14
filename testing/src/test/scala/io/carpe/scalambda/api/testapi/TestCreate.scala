package io.carpe.scalambda.api.testapi

import cats.effect.IO
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.fixtures.TestModels.Car
import io.carpe.scalambda.response.ApiError

class TestCreate extends ApiResource.Create[TestApi, Car] {

  /**
   * Create a record
   *
   * @param input for request
   * @return an IO Monad that wraps logic for attempting to create the record
   */
  override def create(input: Car)(implicit api: TestApi): IO[Car] = IO {
    if (input.hp < 42) {
      throw new ApiError {
        override val httpStatus: Int = 422

        /**
         * The plain text string of what happened. Great for humans, bad for systems - use errorCodes when a system needs
         * to respond to the error in a specific way.
         */
        override val message: String = "Not enough horsepower"
      }
    }

    input
  }
}
