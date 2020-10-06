package io.carpe.scalambda

import sbt._

object XRaySettings {

  lazy val xrayRecorderVersion = "2.4.0"

  def xrayLibs(isXrayEnabled: Boolean): Seq[ModuleID] = {
    if (isXrayEnabled) {
      return Seq(
        "com.amazonaws" % "aws-xray-recorder-sdk-core" % xrayRecorderVersion,
        "com.amazonaws" % "aws-xray-recorder-sdk-aws-sdk-v2" % xrayRecorderVersion,
        "com.amazonaws" % "aws-xray-recorder-sdk-aws-sdk-v2-instrumentor" % xrayRecorderVersion
      )
    }
    Seq.empty
  }

}
