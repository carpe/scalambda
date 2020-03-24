package io.carpe.scalambda.effect

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.circe.{Decoder, Encoder}

abstract class ScalambdaIO[I, O](implicit val decoder: Decoder[I], val encoder: Encoder[O]) extends Scalambda[I, O] {

  override def handleRequest(input: I, context: Context): O = {
    run(input, context).attempt.unsafeRunSync()
      .fold(e => throw e, identity)
  }

  def run(i: I, context: Context): IO[O]
}
