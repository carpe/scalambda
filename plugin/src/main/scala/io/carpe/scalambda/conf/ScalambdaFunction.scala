package io.carpe.scalambda.conf

import io.carpe.scalambda.conf.api.ApiGatewayConfig
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.conf.utils.StringUtils

sealed trait ScalambdaFunction {
  def environmentVariables: Seq[EnvironmentVariable]
  def runtime: Option[ScalambdaRuntime]
  def architecture: Option[ArchitectureConfig]
  def terraformLambdaResourceName: String

  /**
   * Create a variable in the outputted yaml that the terraform will inject the actual arn into once the lambda has
   * been created.
   *
   * This makes sure that our swagger file uses the proper lambda invocation ARN.
   */
  lazy val swaggerVariableName: String = {
    s"${terraformLambdaResourceName}_invoke_arn"
  }
}

object ScalambdaFunction {

  case class DefinedFunction(naming: FunctionNaming,
                             handlerPath: String,
                             functionSource: FunctionSource,
                             iamRole: FunctionRoleSource,
                             runtimeConfig: RuntimeConfig,
                             vpcConfig: VpcConf,
                             warmerConfig: WarmerConfig,
                             environmentVariables: Seq[EnvironmentVariable]
                            ) extends ScalambdaFunction {

    /**
     * A relatively static string that should resemble the what the final function name will be. The actual function name
     * used by terraform will depend on the kind of [[FunctionNaming]] method in use by the function, this name is just
     * used terraform outputs and resource names.
     */
    lazy val approximateFunctionName: String = {
      naming match {
        case FunctionNaming.WorkspaceBased =>
          FunctionNaming.inferLambdaName(handlerPath)
        case Static(name) =>
          name
      }
    }

    /**
     * This will be the name of the terraform lambda resource generated by the "scalambdaTerraform" task.
     */
    lazy val terraformLambdaResourceName: String = {
      val snakeCaseFunctionName = StringUtils.toSnakeCase(approximateFunctionName)
      s"${snakeCaseFunctionName}_lambda"
    }

    /**
     * This will be the name of the terraform resource representing the s3 bucket item that is the lambda's code
     */
    lazy val terraformS3BucketItemResourceName: String = {
      s"${StringUtils.toSnakeCase(approximateFunctionName)}_code"
    }

    override def runtime: Option[ScalambdaRuntime] = Some(runtimeConfig.runtime)

    override def architecture: Option[ArchitectureConfig] = Some(runtimeConfig.architecture)
  }


  /**
   * Used to connect external Lambda functions to the generated API Gateway definition
   *
   * @param apiGatewayConf api gateway configuration
   */
  case class ReferencedFunction(functionName: String, qualifier: String, apiGatewayConf: ApiGatewayConfig) extends ScalambdaFunction {
    override def environmentVariables: Seq[EnvironmentVariable] = Seq.empty

    override def terraformLambdaResourceName: String = StringUtils.toSnakeCase(functionName)

    override def runtime: Option[ScalambdaRuntime] = None

    override def architecture: Option[ArchitectureConfig] = None
  }

}