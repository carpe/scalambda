package io.carpe.scalambda.fixtures

import java.io.File

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.{ApiGatewayConf, FunctionConf, Method}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "CarsIndex", "io.cars.index.CarsIndex",
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = Some(ApiGatewayConf(route = "/cars", method = Method.GET)),
      assemblyPath = new File("testingtesting"),
      s3BucketName = "testing"
    )
  }

  lazy val driveCarFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "DriveCar", "io.cars.lambda.DriveCar",
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = None,
      assemblyPath = new File("testingtesting"),
      s3BucketName = "testing"
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction = {
    ScalambdaFunction(
      "FlyPlane", "io.plane.lambda.FlyPlane",
      functionConfig = FunctionConf.carpeDefault.copy(memory = 256, timeout = 30),
      apiConfig = None,
      assemblyPath = new File("testingtesting"),
      s3BucketName = "testing"
    )
  }
}
