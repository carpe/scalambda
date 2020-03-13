package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.ScalambdaFunction.ProjectFunction
import io.carpe.scalambda.conf.function.EnvironmentVariable
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._

case class LambdaFunction(scalambdaFunction: ProjectFunction, s3Bucket: S3Bucket, s3BucketItem: S3BucketItem, dependenciesLayer: LambdaLayerVersion) extends Resource {
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
   * Create a variable in the outputted yaml that the terraform will inject the actual arn into once the lambda has
   * been created.
   *
   * This makes sure that our swagger file uses the proper lambda invocation ARN.
   */
  lazy val swaggerVariableName: String = {
    s"${name}_invoke_arn"
  }

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    // sources for the code this lambda runs
    "s3_bucket" -> TResourceRef("aws_s3_bucket", s3Bucket.name, "id"),
    "s3_key" -> TResourceRef("aws_s3_bucket_object", s3BucketItem.name, "key"),
    "s3_object_version" -> TResourceRef("aws_s3_bucket_object", s3BucketItem.name, "version_id"),
    "source_code_hash" -> TLiteral(s"filebase64sha256(aws_s3_bucket_object.${s3BucketItem.name}.source)"),

    // dependency layer
    "layers" -> TArray(
      TResourceRef("aws_lambda_layer_version", dependenciesLayer.name, "arn")
    ),

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
    }): _*)),

    "vpc_config" -> TBlock(
      "subnet_ids" -> TArray(scalambdaFunction.vpcConfig.subnetIds.map(TString): _*),
      "security_group_ids" -> TArray(scalambdaFunction.vpcConfig.securityGroupIds.map(TString): _*)
    )

  )
}
