package io.carpe.scalambda.conf.keys

import sbt._

trait XRayKeys {
  lazy val enableXray = settingKey[Boolean]("Enables AWS X-Ray for any Lambda functions or Api Gateway stages generated with scalambdaTerraform.")
}
