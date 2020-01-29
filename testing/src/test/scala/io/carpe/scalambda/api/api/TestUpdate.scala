package io.carpe.scalambda.api.api

import cats.effect.IO
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.conf.ApiContext
import io.carpe.scalambda.api.update.UpdateRequest
import io.carpe.scalambda.fixtures.TestModels.Car
import io.carpe.scalambda.response.ApiError

case class TestUpdate() extends ApiResource.Update[Car] {

  /**
   * Update a record
   *
   * @param input for request
   * @return an IO Monad that wraps logic for attempting to update the record
   */
  override def update(input: UpdateRequest[Car])(implicit context: ApiContext): IO[Car] = IO {
    if (input.body.hp < 42) {
      throw new ApiError {
        override val httpStatus: Int = 422

        /**
         * The plain text string of what happened. Great for humans, bad for systems - use errorCodes when a system needs
         * to respond to the error in a specific way.
         */
        override val message: String = "Not enough horsepower"
      }
    }

    // return the original input
    input.body.copy(hp = input.id)
  }
}

