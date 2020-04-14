package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionRoleSource

trait FunctionRoleSourceKeys {
  lazy val roleFromVariable: FunctionRoleSource = FunctionRoleSource.FromVariable
  def roleFromArn(iamRoleArn: String): FunctionRoleSource = FunctionRoleSource.StaticArn(iamRoleArn)
}
