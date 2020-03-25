package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionRoleSource

trait FunctionRoleSourceKeys {
  def fromVariable: FunctionRoleSource = FunctionRoleSource.FromVariable
  def fromArn(iamRoleArn: String): FunctionRoleSource = FunctionRoleSource.StaticArn(iamRoleArn)
}
