package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionRoleSource

trait FunctionRoleSourceKeys {
  lazy val RoleFromVariable: FunctionRoleSource = FunctionRoleSource.RoleFromVariable
  def RoleFromArn(iamRoleArn: String): FunctionRoleSource = FunctionRoleSource.RoleFromArn(iamRoleArn)
}
