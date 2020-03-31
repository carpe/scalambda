import scala.tools.nsc.Properties

// plugin used to get build info at compile time. This is used specifically to synchronize the versions of Scalambda's
// sbt plugin and library.
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// CarpeData's shared project configuration plugin to jumpstart your projects
resolvers += "Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release"
credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials"))
addSbtPlugin("io.carpe" % "sbt-carpe" % "1.0.0")
