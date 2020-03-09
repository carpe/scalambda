package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.terraform.ast.data.ArchiveFile
import io.carpe.scalambda.terraform.ast.resources.{S3Bucket, S3BucketItem}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("CarsIndex"), "io.cars.index.CarsIndex::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = Some(ApiGatewayConf(route = "/cars", method = Method.GET)),
      environmentVariables = List.empty
    )
  }

  lazy val driveCarFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("DriveCar"), "io.cars.lambda.DriveCar::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = None,
      environmentVariables = List(
        EnvironmentVariable.Static("API", "www.google.com")
      )
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction = {
    ScalambdaFunction(
      Static("FlyPlane"), "io.plane.lambda.FlyPlane::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault.copy(memory = 256, timeout = 30),
      apiConfig = None,
      environmentVariables = List.empty
    )
  }

  lazy val s3Bucket: S3Bucket = S3Bucket("testing")
  lazy val s3BucketItem: S3BucketItem = S3BucketItem(s3Bucket, "sources", "sources.zip", ArchiveFile("code", "project.jar", "project.zip"))
}
