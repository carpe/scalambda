package io.carpe.scalambda.conf.keys

import sbt._

trait TerraformKeys {

  lazy val scalambdaTerraformPath = taskKey[File]("Path to where terraform should be written to.")

  lazy val scalambdaTerraform =
    taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")
}
