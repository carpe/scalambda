package io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources

import io.carpe.scalambda.conf.ScalambdaFunction.DefinedFunction
import io.carpe.scalambda.conf.function.{EnvironmentVariable, ScalambdaRuntime}
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}

case class LambdaFunction( scalambdaFunction: DefinedFunction,
                           subnetIds: TValue,
                           securityGroupIds: TValue,
                           version: String,
                           s3Bucket: S3Bucket,
                           s3BucketItem: S3BucketItem,
                           dependenciesLayer: Option[LambdaLayerVersion],
                           isXrayEnabled: Boolean,
                           billingTags: Seq[BillingTag],
                           additionalBillingTagsVariable: TVariableRef
                         ) extends Resource {

  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override lazy val resourceType: String = "aws_lambda_function"

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

  private val runtime: TValue = TString(scalambdaFunction.runtimeConfig.runtime.identifier)
  private val dependencyLayers: TValue = scalambdaFunction.runtimeConfig.runtime match {
    case ScalambdaRuntime.Java8 =>
      dependenciesLayer.map(layer => TArray(TResourceRef(layer, "arn"))).getOrElse(TNone)
    case ScalambdaRuntime.Java11 =>
      dependenciesLayer.map(layer => TArray(TResourceRef(layer, "arn"))).getOrElse(TNone)
    case ScalambdaRuntime.GraalNative =>
      TNone
    case ScalambdaRuntime.LinuxTwo =>
      TNone
  }

  private val architectures: TValue = TString("arm64")

  private val environment: TValue = {
    TBlock("variables" -> TObject({
      scalambdaFunction.environmentVariables :+ (EnvironmentVariable.StaticVariable("SCALAMBDA_VERSION", version))
    }.map {
      case EnvironmentVariable.StaticVariable(key, value) =>
        key -> TString(value)
      case EnvironmentVariable.VariableFromTF(key, variableName) =>
        key -> TVariableRef(variableName)
    }: _*))
  }

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    // sources for the code this lambda runs
    "s3_bucket" -> TResourceRef(s3Bucket, "id"),
    "s3_key" -> TResourceRef(s3BucketItem, "key"),
    "s3_object_version" -> TResourceRef(s3BucketItem, "version_id"),
    "source_code_hash" -> TLiteral(s"filebase64sha256(aws_s3_bucket_object.${s3BucketItem.name}.source)"),
    // dependency layer
    "layers" -> dependencyLayers,
    // role for the lambda
    "role" -> scalambdaFunction.iamRole.asTFValue(scalambdaFunction),
    // name of the lambda
    "function_name" -> scalambdaFunction.naming.asTValue(scalambdaFunction),
    // path to the scalambda function that this function uses as an entrypoint to your code during execution
    "handler" -> TString(scalambdaFunction.handlerPath),
    // amount of memory, in MB, that the function can use at runtime
    "memory_size" -> TNumber(scalambdaFunction.runtimeConfig.memory),
    // runtime for the function
    "runtime" -> runtime,
    // timeout for the function. should be 30 seconds max for api gateway lambdas
    "timeout" -> TNumber(scalambdaFunction.runtimeConfig.timeout),
    // environment variables to inject into the lambda
    "environment" -> environment,
    // vpc configuration for the lambda
    "vpc_config" -> TBlock(
      "subnet_ids" -> subnetIds,
      "security_group_ids" -> securityGroupIds
    ),
    // architecture
    "architecture" -> architectures,
    // sets the lambda function to publish a new version for each change
    "publish" -> TBool(true),
    // xray configuration for the lambda
    "tracing_config" -> TBlock(
      "mode" -> {
        if (isXrayEnabled) {
          TString("PassThrough")
        } else TNone
      }
    ),
    // billing tags is set to a function that merges both the billing tags provided by sbt, as well as the ones
    // provided by the variable
    "tags" -> TFunctionInvocation(functionName = "merge", arguments = Seq(
      TObject(
        billingTags.map(billingTag => {
          billingTag.name -> TString(billingTag.value)
        }): _*),
      additionalBillingTagsVariable
    )),
    "depends_on" -> dependenciesLayer.map(dependenciesLayer => {
      TArray(TLiteral(s"${dependenciesLayer.resourceType}.${dependenciesLayer.name}"))
    }).getOrElse(TNone),
    "reserved_concurrent_executions" -> {
      if (scalambdaFunction.runtimeConfig.reservedConcurrency < 0) {
        TNone
      } else {
        TNumber(scalambdaFunction.runtimeConfig.reservedConcurrency)
      }
    }
  )
}
