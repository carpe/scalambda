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
      "io.symphonia" % "lambda-logging" % "1.0.3",
      "org.apache.logging.log4j" % "log4j-api" % log4jVersion
    ),

    excludeDependencies += "org.slf4j.slf4j-simple"
  )
}
