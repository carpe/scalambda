package io.carpe.scalambda

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

trait Scalambda[I, O] extends RequestHandler[I, O] {

  final override def handleRequest(input: I, context: Context): O = {
    run(input, context)
  }

  def run(input: I, context: Context): O
}
