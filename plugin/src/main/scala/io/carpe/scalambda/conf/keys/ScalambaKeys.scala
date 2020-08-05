package io.carpe.scalambda.conf.keys

import _root_.io.carpe.scalambda.conf.ScalambdaFunction
import _root_.io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import _root_.io.carpe.scalambda.conf.function._
import sbt._

trait ScalambaKeys extends FunctionNamingKeys
  with FunctionRoleSourceKeys
  with VpcConfigKeys
  with ApiGatewayKeys
  with RuntimeKeys
  with WarmerKeys
  with BillingTagKeys
  with AssemblyKeys
  with XRayKeys
  with TerraformKeys
  with EnvironmentVariableKeys {

  lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

  lazy val s3BucketName = settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")

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
    def Function(functionClasspath: String,
                 functionNaming: FunctionNaming = WorkspaceBased,
                 iamRoleSource: FunctionRoleSource = RoleFromVariable,
                 memory: Int = RuntimeConfig.default.memory,
                 runtime: ScalambdaRuntime = RuntimeConfig.default.runtime,
                 concurrencyLimit: Int = RuntimeConfig.default.reservedConcurrency,
                 warmWith: WarmerConfig = WarmerConfig.Cold,
                 vpcConfig: VpcConf = VpcConf.withoutVpc,
                 environmentVariables: Seq[EnvironmentVariable] = List.empty,
                ): ScalambdaFunction.DefinedFunction = {
      ScalambdaFunction.DefinedFunction(
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
}
