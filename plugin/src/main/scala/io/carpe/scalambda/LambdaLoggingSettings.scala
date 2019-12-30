package io.carpe.scalambda

import sbt._
import sbt.Keys.{excludeDependencies, libraryDependencies}
import sbtassembly.AssemblyKeys.{assemblyMergeStrategy, assembly}
import sbtassembly.{MergeStrategy, PathList}

object LambdaLoggingSettings {

  /**
   * Pre-optimized settings for fat-jars that will be used as Lambda Functions
   */
  lazy val loggingSettings: Seq[Def.Setting[_]] = Seq(

    assemblyMergeStrategy in assembly := {
      case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _ => MergeStrategy.first
    },

    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8.2", // bridge: slf4j -> log4j
      "org.apache.logging.log4j" % "log4j-api" % "2.8.2",        // log4j as logging mechanism
      "org.apache.logging.log4j" % "log4j-core" % "2.8.2"        // log4j as logging mechanism
    ),

    excludeDependencies += "org.slf4j.slf4j-simple"
  )
}
