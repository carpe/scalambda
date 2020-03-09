package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.ScalambdaFunctionFixtures
import org.scalatest.flatspec.AnyFlatSpec

class LambdaFunctionSpec extends AnyFlatSpec with ScalambdaFunctionFixtures {

  "LambdaFunction" should "be a serializable terraform resource" in {
    val actual = LambdaFunction(driveCarFunction, s3Bucket = s3Bucket, s3BucketItem = s3BucketItem).toString

    val expected =
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
        |  s3_key = aws_s3_bucket_object.sources.key
        |  memory_size = 1536
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
