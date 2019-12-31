import sbt._
import sbt.Keys.libraryDependencies


ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "io.carpe"
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

//lazy val root = project
//  .in(file("."))
//  .settings(name := "scalambda")
//  .aggregate(core, plugin)

lazy val core = project
  .settings(name := "scalambda-core")
  .enablePlugins(CarpeCorePlugin)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // Circe is a serialization library that supports Scala's case classes much better than Jackson (and is also quite a bit faster)
    libraryDependencies ++= carpeDeps.circe,

    // Cats, which provides extensions to allow for safer, faster functional programming code.
    libraryDependencies ++= carpeDeps.minimalCats,

    // Minimal set of interfaces for AWS Lambda creation
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",

    // Logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",

    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )

lazy val testing = project
  .settings(name := "scalambda-testing")
  .enablePlugins(CarpeCorePlugin)
  .settings(description := "Utilities for testing Lambda Functions created with Scalambda.")
  .settings(
    // Testing
//    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Runtime,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
  ).dependsOn(core)

lazy val plugin = project
  .settings(name := "sbt-scalambda")
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .enablePlugins(CarpeCorePlugin)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // this allows the plugin see what the current version of scalambda is at runtime in order to
    // automatically include the library as a dependency.
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "io.carpe.scalambda",

    libraryDependencies ++= {
      // get sbt and scala version used in build to resolve plugins with
      val sbtVersion     = (sbtBinaryVersion in pluginCrossBuild).value
      val scalaVersion   = (scalaBinaryVersion in update).value

      // the plugins we want to include
      Seq(
        // Plugin for building fat-jars
        "com.eed3si9n" % "sbt-assembly" % "0.14.9",

        // Plugin for deploying fat-jars as AWS Lambda Functions
        "com.gilt.sbt" % "sbt-aws-lambda" % "0.7.0"
      ).map(Defaults.sbtPluginExtra(_, sbtVersion, scalaVersion))
    }
  )