package io.carpe.scalambda.testing.api

import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.testing.api.behaviors.ApiResourceBehaviors
import io.carpe.scalambda.testing.api.resourcehandlers.MockApiResourceHandling
import org.scalatest.flatspec.AnyFlatSpec

trait MockApiResourceSpec[C <: ScalambdaApi] extends ApiResourceBehaviors[C] with MockApiResourceHandling[C] {
  this: AnyFlatSpec =>
}

