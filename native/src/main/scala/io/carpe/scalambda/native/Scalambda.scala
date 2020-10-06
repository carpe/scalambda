package io.carpe.scalambda.native

import cats.effect.IO
import io.circe.{Decoder, Encoder}

abstract class Scalambda[I, O](implicit override val decoder: Decoder[I], override val encoder: Encoder[O]) extends ScalambdaIO[I, O] {
  /**
   * Wrap the eval for folks who don't like dealing with cats.effect.IO monads. For the record, I fully agree this is
   * kind of a dumb way of doing this, but you gotta admit, it is pretty darn effective.
   *
   * @param input that was decoded from the event source
   * @return
   */
  override def run(input: I): IO[O] = IO { eval(input) }

  /**
   * Called each time the Lambda is invoked. All work your Lambda performs should be performed inside the IO returned
   * by this function.
   *
   * @param input that was decoded from the event source
   * @return
   */
  def eval(input: I): O
}
