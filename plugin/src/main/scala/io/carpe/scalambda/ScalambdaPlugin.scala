package io.carpe.scalambda

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
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.autoImport.{assemblyDefaultJarName, assemblyJarName, assemblyOption, assemblyOutputPath, assemblyPackageDependency}
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

    lazy val apiName = settingKey[String]("Prefix for the name of the api. Defaults to project name")

    lazy val s3BucketName =
      settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")
    lazy val domainName = settingKey[String]("Domain name to be used in the terraform output")
    lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

    lazy val enableXray = settingKey[Boolean]("Enables AWS X-Ray for any Lambda functions or Api Gateway stages generated with scalambdaTerraform.")

    lazy val scalambdaTerraformPath = settingKey[File]("Path to where terraform should be written to.")

    lazy val packageScalambdaMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val packageScalambda = taskKey[File]("Create jar (without dependencies) for your Lambda Function(s)")

    lazy val packageScalambdaDependenciesMergeStrat =
      settingKey[String => MergeStrategy]("mapping from archive member path to merge strategy")
    lazy val packageScalambdaDependencies = taskKey[File](
      "Create a jar containing all the dependencies for your Lambda Function(s). This will be used as a Lambda Layer to support your function."
    )

    lazy val scalambdaTerraform =
      taskKey[Unit]("Produces a terraform module from the project's scalambda configuration.")

    def functionNaming: FunctionNaming.type = FunctionNaming
    def iamRoleSource: FunctionRoleSource.type = FunctionRoleSource
    def functionSource: FunctionSource.type = FunctionSource
    def environmentVariable: EnvironmentVariable.type = EnvironmentVariable
    val Vpc: VpcConf.type = VpcConf
    val Auth: AuthConf.type = AuthConf

    def scalambda( functionClasspath: String,
                   functionNaming: FunctionNaming = WorkspaceBased,
                   iamRoleSource: FunctionRoleSource = FromVariable,
                   functionConfig: FunctionConf = FunctionConf.default,
                   provisionedConcurrency: Int = 0,
                   vpcConfig: VpcConf = Vpc.withoutVpc,
                   environmentVariables: Seq[EnvironmentVariable] = List.empty
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.Function(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            functionConfig = functionConfig,
            provisionedConcurrency = provisionedConcurrency,
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
      invokeArn: String,
      apiConfig: ApiGatewayConf
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.ReferencedFunction(
            functionName = functionName,
            qualifier = qualifier,
            functionArn = invokeArn,
            apiGatewayConf = apiConfig
          )
        }
      )

      // return a project
      awsLambdaProxyPluginConfig ++ scalambdaLibs
    }

    def scalambdaEndpoint( functionClasspath: String,
                           functionNaming: FunctionNaming = WorkspaceBased,
                           iamRoleSource: FunctionRoleSource = FromVariable,
                           functionConfig: FunctionConf = FunctionConf.apiDefault,
                           environmentVariables: Seq[EnvironmentVariable] = List.empty,
                           provisionedConcurrency: Int = 0,
                           vpcConfig: VpcConf = Vpc.withoutVpc,
                           apiConfig: ApiGatewayConf
    ): Seq[Def.Setting[_]] = {

      val awsLambdaProxyPluginConfig = Seq(
        // add this lambda to the list of existing lambda definitions for this function
        scalambdaFunctions += {
          ScalambdaFunction.ApiFunction(
            naming = functionNaming,
            handlerPath = functionClasspath + "::handler",
            functionSource = IncludedInModule,
            iamRole = iamRoleSource,
            functionConfig = functionConfig,
            vpcConfig = vpcConfig,
            provisionedConcurrency = provisionedConcurrency,
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
    Seq(
      // set scalambda functions to empty list initially. lambda functions can then be added by users
      scalambdaFunctions := List.empty,
      // builds the lambda function jar without dependencies (so we can bake them in as a separate lambda layer)
      packageScalambdaMergeStrat := {
        case _ => MergeStrategy.last
      },
      packageScalambda := Assembly.assemblyTask(packageScalambda).value,
      assembledMappings in packageScalambda := Assembly.assembledMappingsTask(packageScalambda).value,
      test in packageScalambda := (test in Test).value,
      assemblyOption in packageScalambda := {
        val ao = (assemblyOption in assembly).value
        ao.copy(includeScala = false, includeDependency = false, mergeStrategy = packageScalambdaMergeStrat.value)
      },
      packageOptions in packageScalambda := (packageOptions in (Compile, packageBin)).value,
      assemblyOutputPath in packageScalambda := {
        (target in assembly).value / (assemblyJarName in packageScalambda).value
      },
      assemblyJarName in packageScalambda := (assemblyJarName in packageScalambda)
        .or(assemblyDefaultJarName in packageScalambda)
        .value,
      assemblyDefaultJarName in packageScalambda := { name.value + "-assembly-" + version.value + ".jar" },
      // builds the dependencies of the lambda version. these will be baked into a lambda layer to improve deployment times
      packageScalambdaDependenciesMergeStrat := {
        case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
        case PathList("META-INF", "MANIFEST.MF")                 => MergeStrategy.discard
        case "log4j2.xml"                                        => MergeStrategy.discard
        case _ =>
          MergeStrategy.last
      },
      packageScalambdaDependencies := Assembly.assemblyTask(packageScalambdaDependencies).value,
      assembledMappings in packageScalambdaDependencies := Assembly
        .assembledMappingsTask(packageScalambdaDependencies)
        .value,
      test in packageScalambdaDependencies := (test in packageScalambda).value,
      assemblyOption in packageScalambdaDependencies := {
        val ao = (assemblyOption in assemblyPackageDependency).value
        ao.copy(
          includeBin = false,
          includeScala = true,
          includeDependency = true,
          mergeStrategy = packageScalambdaDependenciesMergeStrat.value
        )
      },
      packageOptions in packageScalambdaDependencies := (packageOptions in (Compile, packageBin)).value,
      assemblyOutputPath in packageScalambdaDependencies := {
        (target in assembly).value / (assemblyJarName in packageScalambdaDependencies).value
      },
      assemblyJarName in packageScalambdaDependencies := (assemblyJarName in packageScalambdaDependencies)
        .or(assemblyDefaultJarName in packageScalambdaDependencies)
        .value,
      assemblyDefaultJarName in packageScalambdaDependencies := {
        name.value + "-assembly-" + version.value + "-deps.jar"
      },
      scalambdaTerraformPath := target.value / "terraform",
      scalambdaTerraform := {
        ScalambdaTerraform.writeTerraform(
          projectName = { sbt.Keys.name.value },
          functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
          version = gitHeadCommit.value.getOrElse({ formattedDateVersion.value }),
          s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas"),
          projectSource = { packageScalambda.value },
          dependencies = { packageScalambdaDependencies.value },
          isXrayEnabled = enableXray.?.value.getOrElse(false),
          apiName = apiName.?.value.getOrElse(s"${sbt.Keys.name.value}"),
          terraformOutput = scalambdaTerraformPath.value,
          maybeDomainName = domainName.?.value
        )
      },
      libraryDependencies ++= {
        XRaySettings.xrayLibs(isXrayEnabled = enableXray.value)
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
