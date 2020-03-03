package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.fixtures.ScalambdaFunctionFixtures
import io.carpe.scalambda.terraform.ast.resources.LambdaFunction
import org.scalatest.flatspec.AnyFlatSpec

class TerraformFileSpec extends AnyFlatSpec with ScalambdaFunctionFixtures {

  "TerraformFile (containing a valid Definition)" should "be serializable" in {
    val testFile = TerraformFile(Seq(
      LambdaFunction(driveCarFunction)
    ), "lambdas.tf")

    val expectation =
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  s3_key = aws_s3_bucket_object.drive_car.key
        |  memory_size = 1536
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar"
        |  runtime = "java8"
        |}
        |""".stripMargin

    assert(testFile.toString === expectation)
  }

  "TerraformFile (containing multiple valid Definitions)" should "be serializable" in {
    val testFile = TerraformFile(Seq(
      LambdaFunction(driveCarFunction),
      LambdaFunction(flyPlaneFunction)
    ), "lambdas.tf")

    val expectation =
      """resource "aws_lambda_function" "drive_car_lambda" {
        |  function_name = "DriveCar"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  s3_key = aws_s3_bucket_object.drive_car.key
        |  memory_size = 1536
        |  timeout = 900
        |  handler = "io.cars.lambda.DriveCar"
        |  runtime = "java8"
        |}
        |
        |resource "aws_lambda_function" "fly_plane_lambda" {
        |  function_name = "FlyPlane"
        |  s3_bucket = aws_s3_bucket.testing.id
        |  s3_key = aws_s3_bucket_object.fly_plane.key
        |  memory_size = 256
        |  timeout = 30
        |  handler = "io.plane.lambda.FlyPlane"
        |  runtime = "java8"
        |}
        |""".stripMargin

    assert(testFile.toString === expectation)
  }
}
