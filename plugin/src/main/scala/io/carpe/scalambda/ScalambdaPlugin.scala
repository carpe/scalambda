package io.carpe.scalambda

import _root_.io.carpe.scalambda.assembly.AssemblySettings
import _root_.io.carpe.scalambda.conf.ScalambdaFunction
import _root_.io.carpe.scalambda.conf.api.ApiGatewayConfig
import _root_.io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import _root_.io.carpe.scalambda.conf.function._
import _root_.io.carpe.scalambda.conf.keys.ScalambaKeys
import _root_.io.carpe.scalambda.terraform.ScalambdaTerraform
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.GitKeys.{formattedDateVersion, gitHeadCommit}
import sbt.Keys._
import sbt.{Def, _}
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

    @deprecated(message = "Use StaticVariable or VariableFromTF instead", since = "5.0.0")
    def environmentVariable: EnvironmentVariable.type = EnvironmentVariable

    /**
     * Use this function to define a Lambda Function. This will allow you to generate Terraform, build optimized jars,
     * and provide you with helpful libraries to get your Lambda Function deployed.
     *
     * @param functionClasspath    path to the class that contains the handler for your lambda function
     * @param functionNaming       controls how your lambda function is named
     * @param iamRoleSource        controls how your lambda function receives it's IAM role
     * @param memory               amount of memory for your function to use (in MBs)
     * @param runtime              runtime for your function to use (java8 or java11)
     * @param concurrencyLimit     maximum number of concurrent instances of your Function
     * @param warmWith             controls how your lambda function will be kept "warm"
     * @param vpcConfig            use this setting if you need to run your Lambda Function inside a VPC
     * @param environmentVariables use this to inject ENV variables into your Lambda Function
     * @return
     */
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
        // assemble both the project's source and dependencies sequentially to avoid problems with sbt-assembly
        Def.sequential(
          // assembly dependencies
          scalambdaPackageDependencies,
          // assemble source jar
          scalambdaPackage
        ).value

        // write terraform
        ScalambdaTerraform.writeTerraform(
          projectName = {
            sbt.Keys.name.value
          },
          functions = scalambdaFunctions.?.value.map(_.toList).getOrElse(List.empty),
          version = gitHeadCommit.value.getOrElse({
            formattedDateVersion.value
          }),
          s3BucketName = s3BucketName.?.value.getOrElse(s"${sbt.Keys.name.value}-lambdas"),
          billingTags = billingTags.?.value.getOrElse(Nil),
          isXrayEnabled = enableXray.?.value.getOrElse(false),
          apiName = apiName.?.value.getOrElse(s"${sbt.Keys.name.value}"),
          terraformOutput = scalambdaTerraformPath.value,
          domainNameMapping = domainName.?.value.getOrElse(ApiDomain.Unmapped)
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
