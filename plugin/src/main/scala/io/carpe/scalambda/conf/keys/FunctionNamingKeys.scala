package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionNaming

trait FunctionNamingKeys {

  def WorkspaceBased: FunctionNaming = FunctionNaming.WorkspaceBased

  def Static(name: String): FunctionNaming = FunctionNaming.Static(name)
}
