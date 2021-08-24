package io.carpe.scalambda.conf.keys

import sbt._

trait XRayKeys {

  @deprecated(message = "use enable_xray variable on the terraform module that scalambda generates instead.", since = "6.4.0")
  lazy val enableXray = settingKey[Boolean]("Enables AWS X-Ray for any Lambda functions or Api Gateway stages generated with scalambdaTerraform.")
}
