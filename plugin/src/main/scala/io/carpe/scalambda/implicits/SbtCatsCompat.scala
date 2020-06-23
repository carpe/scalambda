package io.carpe.scalambda.implicits

import cats.data.Chain
import sbt.Append

/**
 * Contains definitions of typeclasses in order to enhance compatibility of cats within SBT.
 */
trait SbtCatsCompat {

  // Definitions to allow Chains to be used as concatenable sbt settings
  implicit def chainAppendValue[A]: Append.Value[Chain[A], A] = (a: Chain[A], b: A) => a.+:(b)
  implicit def chainAppendValues[A]: Append.Values[Chain[A], Chain[A]] = (a: Chain[A], b: Chain[A]) => a.++(b)
}
