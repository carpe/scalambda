package io.carpe.scalambda

import sbt.Keys.{excludeDependencies, libraryDependencies}
import sbt._

object LambdaLoggingSettings {

  lazy val log4jVersion = "2.8.2"

  /**
   * Pre-optimized settings for fat-jars that will be used as Lambda Functions
   */
  lazy val loggingSettings: Seq[Def.Setting[_]] = Seq(

    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion, // bridge: slf4j -> log4j
      "org.apache.logging.log4j" % "log4j-api" % log4jVersion,        // log4j as logging mechanism
      "org.apache.logging.log4j" % "log4j-core" % log4jVersion        // log4j as logging mechanism
    ),

    excludeDependencies += "org.slf4j.slf4j-simple"
  )

  lazy val xrayRecorderVersion = "2.4.0"

  lazy val xrayDependencies = Seq(
    "com.amazonaws" % "aws-xray-recorder-sdk-core" % xrayRecorderVersion,
    "com.amazonaws" % "aws-xray-recorder-sdk-aws-sdk-core" % xrayRecorderVersion,
    "com.amazonaws" % "aws-xray-recorder-sdk-aws-sdk-v2" % xrayRecorderVersion,
    "com.amazonaws" % "aws-xray-recorder-sdk-aws-sdk-v2-instrumentor" % xrayRecorderVersion
  )
}
