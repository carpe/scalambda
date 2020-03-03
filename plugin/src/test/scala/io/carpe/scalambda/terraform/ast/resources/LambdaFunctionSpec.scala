package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.ScalambdaFunctionFixtures
import org.scalatest.flatspec.AnyFlatSpec

class LambdaFunctionSpec extends AnyFlatSpec with ScalambdaFunctionFixtures {

  "LambdaFunction" should "be a serializable terraform resource" in {
    val actual = LambdaFunction(driveCarFunction).toString

    val expected =
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

    assert(actual === expected)
  }
}
