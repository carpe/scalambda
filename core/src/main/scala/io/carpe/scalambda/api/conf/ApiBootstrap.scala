package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context

trait ApiBootstrap[C <: ScalambdaApi] {
  def apply(context: Context): C
}

object ApiBootstrap {
  def apply[C <: ScalambdaApi](a: Context => C): Context => C = a.apply
}
