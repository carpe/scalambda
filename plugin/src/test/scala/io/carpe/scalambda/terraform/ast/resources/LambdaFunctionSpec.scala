package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.function.RuntimeConfig
import io.carpe.scalambda.conf.function.ScalambdaRuntime.Java11
import io.carpe.scalambda.fixtures.{ScalambdaFunctionFixtures, TerraformBehaviors}
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaFunction
import org.scalatest.flatspec.AnyFlatSpec

class LambdaFunctionSpec extends AnyFlatSpec with ScalambdaFunctionFixtures with TerraformBehaviors {

  "LambdaFunction" should behave like {
    printableTerraform(
      LambdaFunction(
        driveCarFunction.copy(runtimeConfig = RuntimeConfig.default.copy(runtime = Java11)),
        subnetIds = TArray(),
        securityGroupIds = TArray(),
        version = "42",
        s3Bucket = s3Bucket,
        s3BucketItem = sourcesBucketItem,
        dependenciesLayer = Some(dependenciesLambdaLayer),
        isXrayEnabled = false,
        billingTags = Nil,
        additionalBillingTagsVariable = billingTagsVariable
      ),
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  layers = [
        |    aws_lambda_layer_version.dependency_layer.arn
        |  ]
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  tags = merge({}, var.moar_billing_tags)
        |  depends_on = [
        |    aws_lambda_layer_version.dependency_layer
        |  ]
        |  memory_size = 1536
        |  source_code_hash = filebase64sha256(aws_s3_bucket_object.sources.source)
        |  publish = true
        |  s3_object_version = aws_s3_bucket_object.sources.version_id
        |  environment {
        |    variables = {
        |      API = "www.google.com"
        |      SCALAMBDA_VERSION = "42"
        |    }
        |  }
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar::handler"
        |  runtime = "java11"
        |}""".stripMargin
    )
  }

  "LambdaFunction (when provided vpc_config)" should behave like {
    printableTerraform(
      LambdaFunction(
        flyPlaneFunction,
        subnetIds = TArray(TString("abc123")),
        securityGroupIds = TArray(TString("def456")),
        version = "1337",
        s3Bucket = s3Bucket,
        s3BucketItem = sourcesBucketItem,
        dependenciesLayer = Some(dependenciesLambdaLayer),
        isXrayEnabled = false,
        billingTags = Nil,
        additionalBillingTagsVariable = billingTagsVariable
      ),
      """resource "aws_lambda_function" "fly_plane_lambda" {
        |  layers = [
        |    aws_lambda_layer_version.dependency_layer.arn
        |  ]
        |  function_name = "FlyPlane"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  vpc_config {
        |    subnet_ids = [
        |      "abc123"
        |    ]
        |    security_group_ids = [
        |      "def456"
        |    ]
        |  }
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  tags = merge({}, var.moar_billing_tags)
        |  depends_on = [
        |    aws_lambda_layer_version.dependency_layer
        |  ]
        |  memory_size = 256
        |  source_code_hash = filebase64sha256(aws_s3_bucket_object.sources.source)
        |  publish = true
        |  s3_object_version = aws_s3_bucket_object.sources.version_id
        |  environment {
        |    variables = {
        |      SCALAMBDA_VERSION = "1337"
        |    }
        |  }
        |  timeout = 30
        |  handler = "io.plane.lambda.FlyPlane::handler"
        |  runtime = "java11"
        |}""".stripMargin
    )
  }

  "LambdaFunction (when provided billing tags)" should behave like {
    printableTerraform(
      LambdaFunction(
        driveCarFunction,
        subnetIds = TArray(),
        securityGroupIds = TArray(),
        version = "1337",
        s3Bucket = s3Bucket,
        s3BucketItem = sourcesBucketItem,
        dependenciesLayer = Some(dependenciesLambdaLayer),
        isXrayEnabled = false,
        billingTags = Seq(BillingTag("YourMomma", "SoFat")),
        additionalBillingTagsVariable = billingTagsVariable
      ),
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  layers = [
        |    aws_lambda_layer_version.dependency_layer.arn
        |  ]
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  tags = merge({
        |    YourMomma = "SoFat"
        |  }, var.moar_billing_tags)
        |  depends_on = [
        |    aws_lambda_layer_version.dependency_layer
        |  ]
        |  memory_size = 1536
        |  source_code_hash = filebase64sha256(aws_s3_bucket_object.sources.source)
        |  publish = true
        |  s3_object_version = aws_s3_bucket_object.sources.version_id
        |  environment {
        |    variables = {
        |      API = "www.google.com"
        |      SCALAMBDA_VERSION = "1337"
        |    }
        |  }
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar::handler"
        |  runtime = "java11"
        |}""".stripMargin
    )
  }

}
