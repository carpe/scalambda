package io.carpe.scalambda

import _root_.io.carpe.scalambda.conf.ScalambdaFunction
import _root_.io.carpe.scalambda.conf.function.{ApiGatewayConf, FunctionConf, FunctionRoleSource}
import _root_.io.carpe.scalambda.conf.function.FunctionRoleSource.FromVariable
import _root_.io.carpe.scalambda.terraform.ScalambdaTerraform
import com.typesafe.sbt.GitVersioning
import sbt.Keys.{baseDirectory, credentials, libraryDependencies, resolvers}
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
    lazy val functionNamePrefix = settingKey[String]("Prefix to prepend onto the names of any AWS Functions defined and deployed via Scalambda.")
    lazy val s3BucketName = settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")
    lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

    lazy val scalambdaTerraformPath = settingKey[File]("Path to where terraform should be written to.")

    lazy val packageScalambda = taskKey[File]("Use sbt-assembly to create jar for your Lambda Function(s)")
    lazy val scalambdaTerraform = taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")

    private def inferLambdaName(functionPrefix: Option[String], functionClasspath: String) = {
      functionPrefix.getOrElse("") + functionClasspath.split('.').last
    }

    def scalambda(functionClasspath: String, functionConfig: FunctionConf = FunctionConf.carpeDefault): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          val inferredFunctionName = inferLambdaName(functionNamePrefix.?.value, functionClasspath)
          ScalambdaFunction(
            functionName = inferredFunctionName,
            handlerPath = functionClasspath + "::handler",
            iamRole = FromVariable(ScalambdaFunction.terraformLambdaResourceName(inferredFunctionName), s"Arn for the IAM Role to be used by the ${inferredFunctionName} Lambda Function."),
            functionConfig = functionConfig,
            apiConfig = None,
            s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas")
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def scalambdaApi(functionClasspath: String, functionConfig: FunctionConf = FunctionConf.carpeDefault, apiConfig: ApiGatewayConf): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          val inferredFunctionName = inferLambdaName(functionNamePrefix.?.value, functionClasspath)
          ScalambdaFunction(
            functionName = inferredFunctionName,
            handlerPath = functionClasspath + "::handler",
            iamRole = FromVariable(ScalambdaFunction.terraformLambdaResourceName(inferredFunctionName), s"Arn for the IAM Role to be used by the ${inferredFunctionName} Lambda Function."),
            functionConfig = functionConfig,
            apiConfig = Some(apiConfig),
            s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas")
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }
  }

  import autoImport._

  override def requires: Plugins = AssemblyPlugin && GitVersioning

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // set scalambda functions to empty list initially. lambda functions can then be added by users
    scalambdaFunctions := List.empty,

    packageScalambda := sbtassembly.AssemblyKeys.assembly.value,

    scalambdaTerraformPath := baseDirectory.value / "terraform" / sbt.Keys.name.value,

    scalambdaTerraform := {


      ScalambdaTerraform.writeTerraform(
        functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
        assemblyOutput = { packageScalambda.value },
        terraformOutput = scalambdaTerraformPath.value
      )
    }
  ) ++ LambdaLoggingSettings.loggingSettings

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials")),
    resolvers += "Carpe Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release",
    resolvers += "Carpe Artifactory Realm Snapshots" at "https://bin.carpe.io/artifactory/sbt-dev"
  )

}
