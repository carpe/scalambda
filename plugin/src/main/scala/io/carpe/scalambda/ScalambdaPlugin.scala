package io.carpe.scalambda

import _root_.io.carpe.scalambda.conf.{QualifiedLambdaArn, ScalambdaFunction}
import _root_.io.carpe.scalambda.conf.function.{ApiGatewayConf, Method, FunctionConf}
import _root_.io.carpe.scalambda.terraform.ScalambdaTerraform
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

    lazy val scalambdaS3BucketPrefix = settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Default to project name.")

    lazy val scalambdaFunctions = taskKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

    lazy val scalambdaPublish = taskKey[List[QualifiedLambdaArn]]("Packages and deploys the current project to an existing AWS Lambda alias")
    lazy val scalambdaTerraformPath = settingKey[Option[String]]("Path to where terraform should be written to.")
    lazy val scalambdaTerraform = taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")

    private def inferLambdaName(functionPrefix: Option[String], functionClasspath: String) = {
      functionPrefix.getOrElse("") + functionClasspath.split('.').last
    }

    def scalambda(functionClasspath: String, functionConfig: FunctionConf = FunctionConf.carpeDefault): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += ScalambdaFunction(
          functionName = inferLambdaName(functionNamePrefix.value, functionClasspath),
          handlerPath = functionClasspath + "::handler",
          functionConfig = functionConfig,
          apiConfig = None,
          assemblyPath = AssemblyKeys.assemblyOutputPath.value,
          s3BucketName = scalambdaS3BucketPrefix.?.value.getOrElse(s"${project.id}-lambdas")
        )
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def scalambdaApi(functionClasspath: String, functionConfig: FunctionConf = FunctionConf.carpeDefault, apiConfig: ApiGatewayConf): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += ScalambdaFunction(
          functionName = inferLambdaName(functionNamePrefix.value, functionClasspath),
          handlerPath = functionClasspath + "::handler",
          functionConfig = functionConfig,
          apiConfig = Some(apiConfig),
          assemblyPath = AssemblyKeys.assemblyOutputPath.value,
          s3BucketName = scalambdaS3BucketPrefix.?.value.getOrElse(s"${project.id}-lambdas")
        )
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
    ),

    scalambdaTerraform := ScalambdaTerraform.writeTerraform(
      rootTerraformPath = scalambdaTerraformPath.value.getOrElse("./terraform/scalambda"),
      functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
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
