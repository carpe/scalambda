import sbt._
import sbt.Keys.{name, scalaBinaryVersion,version, testOptions}
import sbtsonar.SonarPlugin.autoImport.sonarProperties

object sonar {

  lazy val sonarSettings: Seq[Def.Setting[_]] = {
    Seq(
      // test arguments used to produce test reports for sonar to consume
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")
      ),
      // this sets the sonar properties for this project, describing how sonar will be run
      sonarProperties ++= Map(
        "sonar.host.url" -> "https://sonarcloud.io",
        "sonar.organization" -> "carpe",
        "sonar.projectName" -> name.value,
        "sonar.projectKey" -> s"carpe_${name.value}",
        "sonar.projectVersion" -> version.value,
        "sonar.sources" -> "src/main/scala",
        "sonar.tests" -> "src/test/scala",
        // the path to these test reports relies on the testReportArguments exposed by this object being utilized
        "sonar.junit.reportPaths" -> "target/test-reports",
        "sonar.sourceEncoding" -> "UTF-8",
        "sonar.scala.version" -> scalaBinaryVersion.value,
        "sonar.scala.coverage.reportPaths" -> s"target/scala-${scalaBinaryVersion.value}/scoverage-report/scoverage.xml",
        "sonar.scala.scapegoat.reportPaths" -> s"target/scala-${scalaBinaryVersion.value}/scapegoat-report/scapegoat-scalastyle.xml",
        "sonar.login" -> sys.env.getOrElse("SONAR_AUTH_TOKEN", ""),
        "sonar.verbose" -> "true"
      )
    )
  }
}
