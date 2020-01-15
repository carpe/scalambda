package io.carpe.scalambda

import _root_.io.carpe.scalambda.conf.QualifiedLambdaArn
import com.gilt.aws.lambda.AwsLambdaPlugin
import com.gilt.aws.lambda.AwsLambdaPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.GitKeys.{formattedDateVersion, gitHeadCommit}
import sbt.Keys.{credentials, libraryDependencies, resolvers}
import sbt._
import sbtassembly._

import scala.tools.nsc.Properties


object ScalambdaPlugin extends AutoPlugin {

  // get the current version of scalambda via the "sbt-buildinfo" plugin
  val currentScalambdaVersion: String = BuildInfo.version

  lazy val scalambdaLibs = Seq(
    // Scalambda is a lightweight library for building Lambda functions in Scala
    libraryDependencies += "io.carpe" %% "scalambda-core" % currentScalambdaVersion,

    // Testing utilities
    libraryDependencies += "io.carpe" %% "scalambda-testing" % currentScalambdaVersion % Test
  )

  object autoImport {

    lazy val scalambdaAlias = settingKey[Option[String]]("Optional Function Alias to attach to newly deployed Lambda Function versions.")
    lazy val scalambdaRoleArn = settingKey[String]("ARN for AWS Role to use for lambda functions.")
    lazy val functionNamePrefix = settingKey[Option[String]]("Prefix to prepend onto the names of any AWS Functions defined and deployed via Scalambda.")
    lazy val scalambdaPublish = taskKey[List[QualifiedLambdaArn]]("Packages and deploys the current project to an existing AWS Lambda alias")

    private def inferLambdaName(functionPrefix: Option[String], functionClasspath: String) = {
      functionPrefix.getOrElse("") + functionClasspath.split('.').last
    }

    def lambdaFunction(functionClasspath: String): Seq[Def.Setting[_]] = {

      val awsLambdaPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        lambdaHandlers += (inferLambdaName(functionNamePrefix.value, functionClasspath) -> (functionClasspath + "::handler")),
        region := Some("us-west-2"),
        s3Bucket := Some("carpe-lambdas"),
        // you might be asking why this amount of memory. AWS scales how much CPU your Lambdas are executed based on how much
        // memory you give them. As on 6/14/19, at 1792MB a Lambda will run with a full vCPU which ends up saving us money in
        // the long run as it cuts the total runtime of the Lambdas by 80% from 512MB of memory.
        // Furthermore, an increase to 2048MB will reduce coldstart times by up to 300ms!
        // TL;DR Even though the Function only uses ~256MB of memory, keep this number high to provide a better UX.
        awsLambdaMemory := Some(1536),
        // set default timeout to 15 minutes
        awsLambdaTimeout := Some(15 * 60),
        roleArn := Some(scalambdaRoleArn.value)
      )

      // return a project
      awsLambdaPluginConfig ++ scalambdaLibs
    }

    def apiGatewayProxyLambda(functionClasspath: String): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        lambdaHandlers += (inferLambdaName(functionNamePrefix.value, functionClasspath) -> (functionClasspath + "::handler")),
        region := Some("us-west-2"),
        s3Bucket := Some("carpe-lambdas"),
        // you might be asking why this amount of memory. AWS scales how much CPU your Lambdas are executed based on how much
        // memory you give them. As on 6/14/19, at 1792MB a Lambda will run with a full vCPU which ends up saving us money in
        // the long run as it cuts the total runtime of the Lambdas by 80% from 512MB of memory.
        // Furthermore, an increase to 2048MB will reduce coldstart times by up to 300ms!
        // TL;DR Even though the Function only uses ~256MB of memory, keep this number high to provide a better UX.
        awsLambdaMemory := Some(1536),
        awsLambdaTimeout := Some(30),
        roleArn := Some(scalambdaRoleArn.value)
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }
  }

  import autoImport._

  override def requires: Plugins = AwsLambdaPlugin && AssemblyPlugin && GitVersioning

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scalambdaPublish := LambdaTasks.publishLambda(
      deployMethod = deployMethod.value.getOrElse("S3"),
      region = region.value.getOrElse("us-west-2"),
      jar = packageLambda.value,
      s3Bucket = s3Bucket.value.getOrElse("carpe-lambdas"),
      s3KeyPrefix = s3KeyPrefix.?.value.getOrElse(""),
      lambdaName = lambdaName.value,
      handlerName = handlerName.value,
      lambdaHandlers = lambdaHandlers.value,
      versionDescription = gitHeadCommit.value.getOrElse({ formattedDateVersion.value }),
      maybeAlias = scalambdaAlias.value.orElse(sys.env.get("SCALAMBDA_ALIAS"))
    )
  ) ++ LambdaLoggingSettings.loggingSettings

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    // set defualt value for function name prefix
    functionNamePrefix := None,
    credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials")),
    resolvers += "Carpe Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release",
    resolvers += "Carpe Artifactory Realm Snapshots" at "https://bin.carpe.io/artifactory/sbt-dev"
  )

}
