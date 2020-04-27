package io.carpe.scalambda

import _root_.io.carpe.scalambda.assembly.AssemblySettings
import _root_.io.carpe.scalambda.conf.ScalambdaFunction
import _root_.io.carpe.scalambda.conf.function.FunctionNaming.WorkspaceBased
import _root_.io.carpe.scalambda.conf.function.FunctionRoleSource.FromVariable
import _root_.io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import _root_.io.carpe.scalambda.conf.function.{VpcConf, _}
import _root_.io.carpe.scalambda.conf.keys.ScalambaKeys
import _root_.io.carpe.scalambda.terraform.ScalambdaTerraform
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.GitKeys.{formattedDateVersion, gitHeadCommit}
import sbt.Keys._
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

  object autoImport extends ScalambaKeys {

    lazy val s3BucketName =
      settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")
    lazy val domainName = settingKey[String]("Domain name to be used in the terraform output")
    lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

    lazy val enableXray = settingKey[Boolean]("Enables AWS X-Ray for any Lambda functions or Api Gateway stages generated with scalambdaTerraform.")

    lazy val scalambdaTerraformPath = taskKey[File]("Path to where terraform should be written to.")

    lazy val scalambdaPackageMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val scalambdaPackage = taskKey[File]("Create jar (without dependencies) for your Lambda Function(s)")

    lazy val scalambdaDependenciesMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val scalambdaPackageDependencies = taskKey[File](
      "Create a jar containing all the dependencies for your Lambda Function(s). This will be used as a Lambda Layer to support your function."
    )

    lazy val scalambdaTerraform =
      taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")

    def functionNaming: FunctionNaming.type = FunctionNaming
    def iamRoleSource: FunctionRoleSource.type = FunctionRoleSource
    def functionSource: FunctionSource.type = FunctionSource
    def environmentVariable: EnvironmentVariable.type = EnvironmentVariable
    val Auth: AuthConfig.type = AuthConfig

    def scalambda(functionClasspath: String,
                  functionNaming: FunctionNaming = WorkspaceBased,
                  iamRoleSource: FunctionRoleSource = FromVariable,
                  memory: Int = RuntimeConfig.default.memory,
                  runtime: ScalambdaRuntime = RuntimeConfig.default.runtime,
                  concurrencyLimit: Int = RuntimeConfig.default.reservedConcurrency,
                  warmWith: WarmerConfig = WarmerConfig.Cold,
                  vpcConfig: VpcConf = VpcConf.withoutVpc,
                  environmentVariables: Seq[EnvironmentVariable] = List.empty,
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.Function(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            runtimeConfig = RuntimeConfig.default.copy(memory = memory, runtime = runtime, reservedConcurrency = concurrencyLimit),
            warmerConfig = warmWith,
            vpcConfig = vpcConfig,
            environmentVariables = environmentVariables
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def foreignEndpoint(
      functionName: String,
      qualifier: String,
      apiConfig: ApiGatewayConfig
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.ReferencedFunction(
            functionName = functionName,
            qualifier = qualifier,
            apiGatewayConf = apiConfig
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def scalambdaEndpoint(functionClasspath: String,
                          functionNaming: FunctionNaming = WorkspaceBased,
                          iamRoleSource: FunctionRoleSource = FromVariable,
                          memory: Int = RuntimeConfig.apiDefault.memory,
                          runtime: ScalambdaRuntime = RuntimeConfig.apiDefault.runtime,
                          concurrencyLimit: Int = RuntimeConfig.apiDefault.reservedConcurrency,
                          environmentVariables: Seq[EnvironmentVariable] = List.empty,
                          warmWith: WarmerConfig = WarmerConfig.Cold,
                          vpcConfig: VpcConf = VpcConf.withoutVpc,
                          apiConfig: ApiGatewayConfig
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.ApiFunction(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            runtimeConfig = RuntimeConfig.apiDefault.copy(memory = memory, runtime = runtime, reservedConcurrency = concurrencyLimit),
            vpcConfig = vpcConfig,
            warmerConfig = warmWith,
            apiConfig = apiConfig,
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

  override def projectSettings: Seq[Def.Setting[_]] =
    AssemblySettings.sourceJarAssemblySettings ++ AssemblySettings.dependencyAssemblySettings ++ Seq(
      // set scalambda functions to empty list initially. lambda functions can then be added by users
      scalambdaFunctions := List.empty,
      scalambdaTerraformPath := target.value / "terraform",
      scalambdaTerraform := {
        // assemble source jar
        scalambdaPackage.value
        // assembly dependencies
        scalambdaPackageDependencies.value
        // write terraform
        ScalambdaTerraform.writeTerraform(
          projectName = { sbt.Keys.name.value },
          functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
          version = gitHeadCommit.value.getOrElse({ formattedDateVersion.value }),
          s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas"),
          billingTags = billingTags.?.value.getOrElse(Nil),
          isXrayEnabled = enableXray.?.value.getOrElse(false),
          apiName = apiName.?.value.getOrElse(s"${sbt.Keys.name.value}"),
          terraformOutput = scalambdaTerraformPath.value,
          maybeDomainName = domainName.?.value
        )
      },
      libraryDependencies ++= {
        XRaySettings.xrayLibs(isXrayEnabled = enableXray.?.value.getOrElse(false))
      }
    ) ++ LambdaLoggingSettings.loggingSettings

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    credentials += Credentials(
      new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials")
    ),
    resolvers += "Carpe Artifactory Realm".at("https://bin.carpe.io/artifactory/sbt-release"),
    resolvers += "Carpe Artifactory Realm Snapshots".at("https://bin.carpe.io/artifactory/sbt-dev")
  )

}
