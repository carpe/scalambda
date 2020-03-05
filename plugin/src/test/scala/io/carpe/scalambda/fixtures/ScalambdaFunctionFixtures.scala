package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.{ApiGatewayConf, FunctionConf, Method, FunctionRoleSource}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "CarsIndex", "io.cars.index.CarsIndex",
      iamRole = FunctionRoleSource.FromVariable("testing", "this is not real"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = Some(ApiGatewayConf(route = "/cars", method = Method.GET)),
      s3BucketName = "testing"
    )
  }

  lazy val driveCarFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "DriveCar", "io.cars.lambda.DriveCar",
      iamRole = FunctionRoleSource.FromVariable("testing", "this is not real"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = None,
      s3BucketName = "testing"
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "FlyPlane", "io.plane.lambda.FlyPlane",
      iamRole = FunctionRoleSource.FromVariable("testing", "this is not real"),
      functionConfig = FunctionConf.carpeDefault.copy(memory = 256, timeout = 30),
      apiConfig = None,
      s3BucketName = "testing"
    )
  }
}
