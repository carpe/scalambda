package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function.{ApiGatewayConf, FunctionConf, FunctionRoleSource, Method}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("CarsIndex"), "io.cars.index.CarsIndex::handler",
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = Some(ApiGatewayConf(route = "/cars", method = Method.GET)),
      s3BucketName = "testing"
    )
  }

  lazy val driveCarFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("DriveCar"), "io.cars.lambda.DriveCar::handler",
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = None,
      s3BucketName = "testing"
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("FlyPlane"), "io.plane.lambda.FlyPlane::handler",
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault.copy(memory = 256, timeout = 30),
      apiConfig = None,
      s3BucketName = "testing"
    )
  }
}
