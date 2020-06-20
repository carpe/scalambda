package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.FunctionRoleSource

trait FunctionRoleSourceKeys {
  lazy val FromVariable: FunctionRoleSource = FunctionRoleSource.FromVariable
  def StaticArn(iamRoleArn: String): FunctionRoleSource = FunctionRoleSource.StaticArn(iamRoleArn)
}
