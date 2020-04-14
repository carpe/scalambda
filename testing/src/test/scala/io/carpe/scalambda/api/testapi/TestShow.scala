package io.carpe.scalambda.api.testapi

import cats.effect.IO
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.show.ShowRequest
import io.carpe.scalambda.fixtures.TestModels.Car

class TestShow() extends ApiResource.Show[TestApi, Car] {
  /**
   * Get a single record by ID
   *
   * @param input for request
   * @return an IOMonad that wraps the logic for attempting to retrieve the single record, if it exists
   */
  override def show(input: ShowRequest)(implicit api: TestApi): IO[Option[Car]] = {
    IO { api.queryDatabase(input.id) }
  }
}
