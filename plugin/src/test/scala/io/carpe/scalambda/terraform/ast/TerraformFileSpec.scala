package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.fixtures.{ScalambdaFunctionFixtures, TerraformBehaviors}
import org.scalatest.flatspec.AnyFlatSpec

class TerraformFileSpec extends AnyFlatSpec with ScalambdaFunctionFixtures with TerraformBehaviors {

  ("TerraformFile (containing a valid Definition)" should behave).like(
    printableTerraform(
      {
        TerraformFile(
          Seq(
            driveCarFunction
          ).map(asLambdaFunction),
          "lambdas.tf"
        )
      }, {
        """# This file was autogenerated by the scalambdaTerraform command from the Scalambda SBT plugin!
          |
          |resource "aws_lambda_function" "drive_car_lambda" {
          |  layers = [
          |    aws_lambda_layer_version.dependency_layer.arn
          |  ]
          |  function_name = "DriveCar"
          |  s3_bucket = aws_s3_bucket.testing.id
          |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
          |  s3_key = aws_s3_bucket_object.sources.key
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
          |  runtime = "java8"
          |}
          |""".stripMargin
      }
    )
  )

  ("TerraformFile (containing multiple valid Definitions)" should behave).like(
    printableTerraform(
      {
        TerraformFile(
          Seq(
            driveCarFunction,
            flyPlaneFunction
          ).map(asLambdaFunction),
          "lambdas.tf"
        )
      }, {
        """# This file was autogenerated by the scalambdaTerraform command from the Scalambda SBT plugin!
          |
          |resource "aws_lambda_function" "drive_car_lambda" {
          |  layers = [
          |    aws_lambda_layer_version.dependency_layer.arn
          |  ]
          |  function_name = "DriveCar"
          |  s3_bucket = aws_s3_bucket.testing.id
          |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
          |  s3_key = aws_s3_bucket_object.sources.key
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
          |  runtime = "java8"
          |}
          |
          |resource "aws_lambda_function" "fly_plane_lambda" {
          |  layers = [
          |    aws_lambda_layer_version.dependency_layer.arn
          |  ]
          |  function_name = "FlyPlane"
          |  s3_bucket = aws_s3_bucket.testing.id
          |  vpc_config {
          |    subnet_ids = [
          |      "subnet-12345678987654321"
          |    ]
          |    security_group_ids = [
          |      "sg-12345678987654321"
          |    ]
          |  }
          |  role = "arn:aws:iam::12345678900:role/lambda_basic_execution"
          |  s3_key = aws_s3_bucket_object.sources.key
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
          |  runtime = "java8"
          |}
          |""".stripMargin
      }
    )
  )
}
