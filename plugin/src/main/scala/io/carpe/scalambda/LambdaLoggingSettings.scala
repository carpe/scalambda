package io.carpe.scalambda

import sbt.Def
import sbt.Keys.{excludeDependencies, libraryDependencies}

object LambdaLoggingSettings {

  lazy val log4jVersion = "2.17.1"

  /**
   * Pre-optimized settings for fat-jars that will be used as Lambda Functions
   */
  lazy val jvmLoggingSettings: Seq[Def.Setting[_]] = {
    import sbt._

    Seq(
      libraryDependencies ++= Seq(
        // provides LazyLogging trait and more
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
        // comes with custom logger that logs the Request ID and more
        "io.symphonia" % "lambda-logging" % "1.0.3",
        // allows us to route log4j logs over to slf4j
        "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % log4jVersion
      ),
      // exclude dependencies aren't compatible with logback
      excludeDependencies += "org.apache.logging.log4j" % "log4j-core",
      excludeDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl",
      excludeDependencies += "org.slf4j.slf4j-simple"
    )
  }

  lazy val nativeLoggingSettings: Seq[Def.Setting[_]] = {
    import sbt._

    // the lambda logging library does NOT play nicely with graal native. We also can't use log4j2 since it too does not
    // work with graal native.
    //
    // Graal native uses slf4j as its logging API, which logback-classic is an implements quite nicely. We also use
    // scala-logging because of the convenience it offers.
    Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
    )
  }
}
