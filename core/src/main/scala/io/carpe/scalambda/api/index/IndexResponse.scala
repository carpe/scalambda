package io.carpe.scalambda.api.index

import io.circe.Encoder

case class IndexResponse[R](data: List[R])

object IndexResponse {
    import io.circe.generic.semiauto._

    implicit def encoder[R](implicit innerEncoder: Encoder[R]): Encoder[IndexResponse[R]] = deriveEncoder[IndexResponse[R]]
}
