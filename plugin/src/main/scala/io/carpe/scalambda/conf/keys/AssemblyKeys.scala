package io.carpe.scalambda.conf.keys

import sbt._
import sbtassembly.MergeStrategy

/**
 * Tasks and Settings related to the creation of jar and zip files to be used by AWS Lambda.
 */
trait AssemblyKeys {

    lazy val scalambdaPackageMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val scalambdaPackage = taskKey[File]("Create jar (without dependencies) for your Lambda Function(s)")

    lazy val scalambdaDependenciesMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val scalambdaPackageDependencies = taskKey[File](
      "Create a jar containing all the dependencies for your Lambda Function(s). This will be used as a Lambda Layer to support your function."
    )
}
