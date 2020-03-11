package io.carpe.scalambda

import _root_.io.carpe.scalambda.conf.ScalambdaFunction
import _root_.io.carpe.scalambda.conf.function.FunctionNaming.WorkspaceBased
import _root_.io.carpe.scalambda.conf.function.FunctionRoleSource.FromVariable
import _root_.io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import _root_.io.carpe.scalambda.conf.function._
import _root_.io.carpe.scalambda.conf.function.VpcConf
import _root_.io.carpe.scalambda.terraform.ScalambdaTerraform
import com.typesafe.sbt.GitVersioning
import sbt.Keys.{credentials, libraryDependencies, resolvers, target}
import sbt._
import sbtassembly.AssemblyKeys._
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
    lazy val apiName = settingKey[String]("Prefix for the name of the api. Defaults to project name")
    lazy val s3BucketName = settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")
    lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

    lazy val scalambdaTerraformPath = settingKey[File]("Path to where terraform should be written to.")

    lazy val packageScalambda = taskKey[File]("Create jar (without dependencies) for your Lambda Function(s)")
    lazy val packageScalambdaDependencies = taskKey[File]("Create a jar containing all the dependencies for your Lambda Function(s). This will be used as a Lambda Layer to support your function.")
    lazy val scalambdaTerraform = taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")

    def functionNaming: FunctionNaming.type = FunctionNaming
    def iamRoleSource: FunctionRoleSource.type = FunctionRoleSource
    def functionSource: FunctionSource.type = FunctionSource
    def environmentVariable: EnvironmentVariable.type = EnvironmentVariable
    val Endpoint: ApiGatewayConf.type = ApiGatewayConf
    val Vpc: VpcConf.type = VpcConf

    def scalambda(functionClasspath: String, functionNaming: FunctionNaming = WorkspaceBased, iamRoleSource: FunctionRoleSource = FromVariable, functionConfig: FunctionConf = FunctionConf.carpeDefault, vpcConfig: VpcConf = Vpc.withoutVpc, environmentVariables: Seq[EnvironmentVariable] = List.empty): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            functionConfig = functionConfig,
            vpcConfig = vpcConfig,
            apiConfig = None,
            environmentVariables = environmentVariables
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def scalambdaEndpoint(functionClasspath: String, functionNaming: FunctionNaming = WorkspaceBased, iamRoleSource: FunctionRoleSource = FromVariable, functionConfig: FunctionConf = FunctionConf.carpeDefault, environmentVariables: Seq[EnvironmentVariable] = List.empty, vpcConfig: VpcConf = Vpc.withoutVpc, apiConfig: ApiGatewayConf): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            functionConfig = functionConfig,
            vpcConfig = vpcConfig,
            apiConfig = Some(apiConfig),
            environmentVariables = environmentVariables
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

    // builds the lambda function jar without dependencies (so we can bake them in as a separate lambda layer)
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = false),
    packageScalambda := sbtassembly.AssemblyKeys.assembly.value,
    // builds the dependencies of the lambda version. these will be baked into a lambda layer to improve deployment times
    packageScalambdaDependencies := sbtassembly.AssemblyKeys.assemblyPackageDependency.value,

    scalambdaTerraformPath := target.value / "terraform",
    scalambdaTerraform := ScalambdaTerraform.writeTerraform(
      functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
      s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas"),
      projectSource = { packageScalambda.value },
      dependencies = { packageScalambdaDependencies.value },
      apiName = apiName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas"),
      terraformOutput = scalambdaTerraformPath.value
    )
  ) ++ LambdaLoggingSettings.loggingSettings

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials")),
    resolvers += "Carpe Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release",
    resolvers += "Carpe Artifactory Realm Snapshots" at "https://bin.carpe.io/artifactory/sbt-dev"
  )

}
