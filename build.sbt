import sbt._
import sbt.Keys.libraryDependencies

import scala.tools.nsc.Properties
import sbtsonar.SonarPlugin.autoImport.sonarProperties

//ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.10")
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / organization := "io.carpe"
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
//ThisBuild / credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials"))
ThisBuild / useCoursier := false
import com.github.sbt.jacoco.JacocoPlugin.autoImport._

lazy val sonarSettings = Seq(
  sonarProperties ++= Map(
    "sonar.modules" -> "core,testing,plugin",
    //"sonar.scala.coverage.reportPaths" -> "target/scala-2.12/jacoco/report/aggregate/",
    "sonar.coverage.jacoco.xmlReportPaths" -> "target/scala-2.12/jacoco/report/jacoco.xml"
),
  aggregate in sonarScan := false
)

lazy val root = (project in file("."))
  .settings(name := "scalambda")
  .aggregate(plugin, core, testing)
  .settings(sonarSettings)
  .settings(jacocoReportSettings := JacocoReportSettings(title = "Report Title", formats = Seq(JacocoReportFormats.XML)))
  //.settings(sonarScan / skip := true)
  .settings(skip in publish := true, skip in publishLocal := true)

lazy val core = project
  .settings(name := "scalambda-core")
  .enablePlugins(CarpeCorePlugin)
  .settings(sonarSettings)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // Circe is a serialization library that supports Scala's case classes much better than Jackson (and is also quite a bit faster)
    libraryDependencies ++= carpeDeps.circe,

    // Cats, which provides extensions to allow for safer, faster functional programming code.
    libraryDependencies ++= carpeDeps.minimalCats,

    // Cats Effect, used to control side effects and make managing resources (such as database connections) easier
    libraryDependencies += "org.typelevel" %% "cats-effect" % "2.0.0",

    // Minimal set of interfaces for AWS Lambda creation
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",

    // Logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",

    // Testing
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  )

lazy val testing = project
  .settings(name := "scalambda-testing")
  .enablePlugins(CarpeCorePlugin)
  .settings(sonarSettings)
  .settings(description := "Utilities for testing Lambda Functions created with Scalambda.")
  .settings(
    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1",
    libraryDependencies += "org.scalamock" %% "scalamock" % "4.4.0",
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10",

    // Logging
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.8.2",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.8.2",

    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  ).dependsOn(core)

lazy val plugin = project
  .settings(name := "sbt-scalambda")
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .enablePlugins(CarpeCorePlugin)
  .settings(sonarSettings)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // this allows the plugin see what the current version of scalambda is at runtime in order to
    // automatically include the library as a dependency.
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "io.carpe.scalambda",

    // Cats, used mainly to make managing all the AWS requests Scalambda makes a little easier
    libraryDependencies ++= carpeDeps.minimalCats,

    // Used to generate swagger file for terraforming
    libraryDependencies += "io.circe" %% "circe-generic" % "0.12.1",
    libraryDependencies += "io.circe" %% "circe-yaml" % "0.12.0",

      // Used for reading configuration values
    libraryDependencies += "com.typesafe" % "config" % "1.2.1",

    // Pre-loaded SBT Plugins
    libraryDependencies ++= {
      // get sbt and scala version used in build to resolve plugins with
      val sbtVersion     = (sbtBinaryVersion in pluginCrossBuild).value
      val scalaVersion   = (scalaBinaryVersion in update).value

      // the plugins we want to include
      Seq(
        // Plugin for building fat-jars
        "com.eed3si9n" % "sbt-assembly" % "0.14.9",

        // Plugin for accessing git info, used to version lambda functions
        "com.typesafe.sbt" % "sbt-git" % "1.0.0"
      ).map(Defaults.sbtPluginExtra(_, sbtVersion, scalaVersion))
    },

    // Testing
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  )
