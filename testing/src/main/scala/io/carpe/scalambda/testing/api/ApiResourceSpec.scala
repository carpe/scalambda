package io.carpe.scalambda.testing.api

import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.testing.api.behaviors.ApiResourceBehaviors
import io.carpe.scalambda.testing.api.resourcehandlers.DefaultApiResourceHandling
import org.scalatest.flatspec.AnyFlatSpec

trait ApiResourceSpec[C <: ScalambdaApi] extends ApiResourceBehaviors[C] with DefaultApiResourceHandling[C] {
  this: AnyFlatSpec =>
}
