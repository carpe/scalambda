package io.carpe.scalambda.api.testapi

import cats.effect.IO
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.delete.DeleteRequest

class TestDelete() extends ApiResource.Delete[TestApi] {
  /**
   * Delete a record
   *
   * @return an IO Monad that wraps logic for attempting to delete the record
   */
  override def delete(input: DeleteRequest)(implicit api: TestApi): IO[Unit] = {
    IO.unit
  }
}

