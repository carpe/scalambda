package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.EnvironmentVariable
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._

case class LambdaFunction(scalambdaFunction: ScalambdaFunction) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override def resourceType: Option[String] = Some("aws_lambda_function")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = scalambdaFunction.terraformLambdaResourceName

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    // sources for the code this lambda runs
    "s3_bucket" -> TResourceRef("aws_s3_bucket", scalambdaFunction.terraformS3BucketResourceName, "id"),
    "s3_key" -> TResourceRef("aws_s3_bucket_object", scalambdaFunction.terraformS3BucketItemResourceName, "key"),

    // role for the lambda
    "role" -> scalambdaFunction.iamRole.asTFValue(scalambdaFunction),

    // name of the lambda
    "function_name" -> scalambdaFunction.naming.asTValue(scalambdaFunction),

    // path to the scalambda function that this function uses as an entrypoint to your code during execution
    "handler" -> TString(scalambdaFunction.handlerPath),

    // amount of memory, in MB, that the function can use at runtime
    "memory_size" -> TNumber(scalambdaFunction.functionConfig.memory),

    // TODO: evaluate amazon coretto 11
    "runtime" -> TString("java8"),

    "timeout" -> TNumber(scalambdaFunction.functionConfig.timeout),

    "environment" -> TBlock("variables" -> TObject(scalambdaFunction.environmentVariables.flatMap(envVariable => {
      envVariable match {
        case EnvironmentVariable.Static(key, value) =>
          Some(key -> TString(value))
        case EnvironmentVariable.Variable(key, variableName) =>
          Some(key -> TVariableRef(variableName))
      }
    }): _*))

  )
}
