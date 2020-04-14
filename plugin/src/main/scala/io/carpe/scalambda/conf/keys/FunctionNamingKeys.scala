package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionNaming
import io.carpe.scalambda.conf.function.FunctionNaming.{Static, WorkspaceBased}

trait FunctionNamingKeys {

  def workspaceBased: FunctionNaming = WorkspaceBased

  def static(name: String): FunctionNaming = Static(name)
}
