package io.carpe.scalambda.api.testapi

import cats.effect.IO
import io.carpe.scalambda.api.ApiResource
import io.carpe.scalambda.api.index.IndexRequest
import io.carpe.scalambda.api.testapi.TestIndex.TestIndexResponse
import io.carpe.scalambda.fixtures.TestModels.Car
import io.circe.{Decoder, Encoder}

class TestIndex extends ApiResource.Index[TestApi, TestIndexResponse] {

  /**
   * Get multiple records.
   *
   * @param input for request
   * @return an IO Monad that wraps logic for attempting to retrieving the records
   */
  override def index(input: IndexRequest)(implicit api: TestApi): IO[TestIndexResponse] = {
    IO {
      TestIndexResponse(List.empty)
    }
  }
}

object TestIndex {
  import io.circe.generic.semiauto._

  case class TestIndexResponse(data: List[Car])

  implicit val encoder: Encoder[TestIndexResponse] = deriveEncoder[TestIndexResponse]
  implicit val decoder: Decoder[TestIndexResponse] = deriveDecoder[TestIndexResponse]
}
