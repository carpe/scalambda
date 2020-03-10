package io.carpe.scalambda.conf

import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.conf.utils.StringUtils

case class ScalambdaFunction(naming: FunctionNaming,
                             handlerPath: String,
                             functionSource: FunctionSource,
                             iamRole: FunctionRoleSource,
                             functionConfig: FunctionConf,
                             apiConfig: Option[ApiGatewayConf],
                             environmentVariables: Seq[EnvironmentVariable]
                            ) {

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
  lazy val terraformLambdaResourceName: String = ScalambdaFunction.terraformLambdaResourceName(approximateFunctionName)

  /**
   * This will be the name of the terraform resource representing the s3 bucket item that is the lambda's code
   */
  lazy val terraformS3BucketItemResourceName: String = {
    s"${StringUtils.toSnakeCase(approximateFunctionName)}_code"
  }
}

object ScalambdaFunction {


  /**
   * The name that of the lambda_function resource that will be used for a given FunctionName.
   * @param functionName to infer a resource name from
   * @return the resource name
   */
  def terraformLambdaResourceName(functionName: String): String = {
    val snakeCaseFunctionName = StringUtils.toSnakeCase(functionName)
    s"${snakeCaseFunctionName}_lambda"
  }
}