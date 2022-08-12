import sbt.Keys.libraryDependencies
import sbt._
import scapegoat._
import sonar._
import versions._

ThisBuild / scapegoatVersion := "1.4.15"
ThisBuild / organization := "io.carpe"
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

ThisBuild / version := "6.4.0"

val Scala212 = "2.12.16"
val Scala213 = "2.13.8"

lazy val root = (project in file("."))
  .settings(name := "scalambda")
  .settings(crossScalaVersions := Nil)
  .aggregate(plugin, core, testing, native)
  .settings(publish / skip := true, publishLocal / skip := true)
  .settings(sonarSettings, sonarScan / aggregate := false, sonarProperties ++= Map("sonar.modules" -> "core,testing,plugin"))

lazy val commonSettings = sonarSettings ++ scapegoatSettings

lazy val core = project
  .settings(name := "scalambda-core")
  .settings(crossScalaVersions := Seq(Scala212, Scala213))
  .settings(commonSettings)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // Circe is a serialization library that supports Scala's case classes much better than Jackson (and is also quite a bit faster)
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-generic-extras"
    ).map(_ % circeVersion),

    // Cats Effect, used to control side effects and make managing resources (such as database connections) easier
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsVersion,

    // Minimal set of interfaces for AWS Lambda
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",

    // Logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,

    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  )

lazy val native = project
  .settings(name := "scalambda-native")
  .settings(crossScalaVersions := Seq(Scala212, Scala213))
  .settings(commonSettings)
  .settings(
    // Circe is a serialization library that we can use to serialize/deserialize requests
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-generic-extras"
    ).map(_ % circeVersion),

    // Cats Effect, used to control side effects and make managing resources (such as database connections) easier
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsVersion,

    // Requests is a simple lib for managing http requests that can be safely run in graal native
    libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.1",

    // Logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,

    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  )

lazy val testing = project
  .settings(name := "scalambda-testing")
  .settings(crossScalaVersions := Seq(Scala212, Scala213))
  .settings(description := "Utilities for testing Lambda Functions created with Scalambda.")
  .settings(
    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion,
    libraryDependencies += "org.scalamock" %% "scalamock" % scalaMockVersion,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10",

    // Logging
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.18.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.18.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.18.0",

    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  ).dependsOn(core)

lazy val plugin = project
  .settings(name := "sbt-scalambda")
  .settings(crossScalaVersions := Seq(Scala212))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(description := "Dependencies shared by both delegates and handlers. Includes things like Models and generic Lambda helpers.")
  .settings(
    // this allows the plugin see what the current version of scalambda is at runtime in order to
    // automatically include the library as a dependency.
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "io.carpe.scalambda",

    // Used to generate swagger file for terraforming
    libraryDependencies += "io.circe" %% "circe-generic" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-yaml" % "0.14.1",

    // Used for reading configuration values
    libraryDependencies += "com.typesafe" % "config" % "1.4.2",

    // used to create zip file for lambda source
    libraryDependencies += "org.apache.commons" % "commons-compress" % "1.21",

    // Pre-loaded SBT Plugins
    libraryDependencies ++= {
      // get sbt and scala version used in build to resolve plugins with
      val sbtVersion     = (pluginCrossBuild / sbtBinaryVersion).value
      val scalaVersion   = (update / scalaBinaryVersion).value

      // the plugins we want to include
      Seq(
        // Plugin for building fat-jars
        "com.eed3si9n" % "sbt-assembly" % "1.2.0",

        // Plugin for accessing git info, used to version lambda functions
        "com.github.sbt" % "sbt-git" % "2.0.0",

        // Used for graal-native assembly (if someone is crazy enough to use it for their lambdas)
        "com.github.sbt" % "sbt-native-packager" % "1.9.10"
      ).map(Defaults.sbtPluginExtra(_, sbtVersion, scalaVersion))
    },

    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/generated/test-reports")
  )

/**
 * Publishing information
 *
 * In order to publish, follow the steps at https://www.scala-sbt.org/release/docs/Using-Sonatype.html
 *
 */

ThisBuild / organization := "io.carpe"
ThisBuild / organizationName := "Carpe Data"
ThisBuild / organizationHomepage := Some(url("https://carpe.io/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/carpe/scalambda"),
    "scm:git@github.com:carpe/scalambda.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "SwiftEngineer",
    name  = "Taylor Brooks",
    email = "taylor.brooks@carpe.io",
    url   = url("https://github.com/SwiftEngineer")
  )
)

ThisBuild / useGpg := true

ThisBuild / description := "Toolkit for building/deploying Lambda Functions with SBT"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/carpe/scalambda"))
ThisBuild / organizationHomepage := Some(url("https://www.carpe.io/"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

/**
 * Documentation / Companion site
 */

lazy val docs = (project in file("docs"))
  .enablePlugins(MicrositesPlugin, GhpagesPlugin)
  .dependsOn(core)
  .settings(
    // set remote repo for Ghpages plugin
    git.remoteRepo := "git@github.com:carpe/scalambda.git",

    // set microsite to use Ghpages plugin for publishing documentation
    micrositePushSiteWith := GHPagesPlugin,

    micrositeHomepage := "https://carpe.github.io/scalambda/",
    micrositeName := "scalambda",

    micrositeAnalyticsToken := "UA-85042842-3",

    // disable gitter since we currently have none
    micrositeGitterChannel := false,

    micrositeBaseUrl := "scalambda",
    micrositeDocumentationUrl := "docs",
    micrositeGithubOwner := "carpe",
    micrositeGithubRepo := "scalambda"
  )