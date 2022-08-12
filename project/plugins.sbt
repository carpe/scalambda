// plugin used to get build info at compile time. This is used specifically to synchronize the versions of Scalambda's
// sbt plugin and library.
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

// used for code coverage reporting
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")

// used for continuous integration and deployment (aka the release process)
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

// sonarqube support
addSbtPlugin("com.sonar-scala" % "sbt-sonar" % "2.3.0")

// used to format scala code
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

// use to do the static code analysis on the code
addSbtPlugin("com.sksamuel.scapegoat" % "sbt-scapegoat" % "1.1.1")

// used for publishing to the sonatype repo
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.13")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

// used to generate the companion site to house documentation
addSbtPlugin("com.47deg"  % "sbt-microsites" % "1.3.4")
