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
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case "log4j2.xml" => MergeStrategy.first
      case _ => MergeStrategy.first
    },

    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",                                 // slf4j as logging interface
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.1" % Runtime, // bridge: slf4j -> log4j
      "org.apache.logging.log4j" % "log4j-api" % "2.11.1" % Runtime,        // log4j as logging mechanism
      "org.apache.logging.log4j" % "log4j-core" % "2.11.1" % Runtime       // log4j as logging mechanism
    ),

    excludeDependencies += "org.slf4j.slf4j-simple"
  )
}
