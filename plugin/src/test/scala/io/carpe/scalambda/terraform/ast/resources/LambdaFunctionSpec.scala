package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.ScalambdaFunctionFixtures
import org.scalatest.flatspec.AnyFlatSpec

class LambdaFunctionSpec extends AnyFlatSpec with ScalambdaFunctionFixtures {

  "LambdaFunction" should "be a serializable terraform resource" in {
    val actual: String = LambdaFunction(driveCarFunction, s3Bucket = s3Bucket, s3BucketItem = sourcesBucketItem, dependenciesLayer = dependenciesLambdaLayer).toString

    val expected: String =
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  layers = [
        |    aws_lambda_layer_version.dependency_layer.arn
        |  ]
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  memory_size = 1536
        |  source_code_hash = filebase64sha256(aws_s3_bucket_object.sources.source)
        |  s3_object_version = aws_s3_bucket_object.sources.version_id
        |  environment {
        |    variables = {
        |      API = "www.google.com"
        |    }
        |  }
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar::handler"
        |  runtime = "java8"
        |}
        |""".stripMargin

    assert(actual === expected)
  }

  it should "be a serializable terraform resource (when provided vpc_config)" in {
    val actual: String = LambdaFunction(driveCarFunction, s3Bucket = s3Bucket, s3BucketItem = sourcesBucketItem, dependenciesLayer = dependenciesLambdaLayer).toString

    val expected: String =
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  layers = [
        |    aws_lambda_layer_version.dependency_layer.arn
        |  ]
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  memory_size = 1536
        |  source_code_hash = filebase64sha256(aws_s3_bucket_object.sources.source)
        |  s3_object_version = aws_s3_bucket_object.sources.version_id
        |  environment {
        |    variables = {
        |      API = "www.google.com"
        |    }
        |  }
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar::handler"
        |  runtime = "java8"
        |}
        |""".stripMargin

    assert(actual === expected)
  }
}
