package io.carpe.scalambda.conf.function

sealed trait Method

object Method {
  case object POST extends Method
  case object GET extends Method
  case object PUT extends Method
  case object DELETE extends Method
}
