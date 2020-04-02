package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function
import io.carpe.scalambda.conf.function.WarmerConfig
import io.circe.Json

trait WarmerKeys {

  def Prov

  def WithJson(json: Json): WarmerConfig.WithJson = function.WarmerConfig.WithJson(json)

  def NoOp: WarmerConfig.NoOp.type = WarmerConfig.NoOp

}
