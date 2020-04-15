// plugin used to get build info at compile time. This is used specifically to synchronize the versions of Scalambda's
// sbt plugin and library.
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "1.5.0")

// used for code coverage reporting
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

// used for continuous integration and deployment (aka the release process)
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

// sonarqube support
addSbtPlugin("com.github.mwz" % "sbt-sonar" % "2.1.0")

// used to format scala code
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1")

// use to do the static code analysis on the code
addSbtPlugin("com.sksamuel.scapegoat" % "sbt-scapegoat" % "1.5.0")

// used for publishing to the sonatype repo
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0-M2")