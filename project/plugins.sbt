import scala.tools.nsc.Properties

// CarpeData's shared project configuration plugin to jumpstart your projects
resolvers += "Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release"
credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials"))
addSbtPlugin("io.carpe" % "sbt-carpe" % "0.0.7")