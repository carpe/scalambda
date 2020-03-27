package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.{ApiFunction, ProjectFunction}
import io.carpe.scalambda.conf.function.AuthConf.{CarpeAuthorizer, Unauthorized}
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.terraform.ast.resources.{LambdaFunction, LambdaLayerVersion, S3Bucket, S3BucketItem}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ApiFunction = {
    ScalambdaFunction.ApiFunction(
      Static("CarsIndex"), "io.cars.index.CarsIndex::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      apiConfig = ApiGatewayConf(route = "/cars", method = Method.GET, authConf = CarpeAuthorizer),
      vpcConfig = VpcConf.withoutVpc,
      environmentVariables = List.empty
    )
  }

  lazy val driveCarFunction: ScalambdaFunction.Function = {
    ScalambdaFunction.Function(
      Static("DriveCar"), "io.cars.lambda.DriveCar::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault,
      vpcConfig = VpcConf.withoutVpc,
      environmentVariables = List(
        EnvironmentVariable.Static("API", "www.google.com")
      )
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction.Function = {
    ScalambdaFunction.Function(
      Static("FlyPlane"), "io.plane.lambda.FlyPlane::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      functionConfig = FunctionConf.carpeDefault.copy(memory = 256, timeout = 30),
      vpcConfig = VpcConf(
        subnetIds = Seq("subnet-12345678987654321"),
        securityGroupIds = Seq("sg-12345678987654321")
      ),
      environmentVariables = List.empty
    )
  }

  lazy val s3Bucket: S3Bucket = S3Bucket("testing")
  lazy val sourcesBucketItem: S3BucketItem = S3BucketItem(s3Bucket, "sources", "sources.jar", "sources.jar", "sources.jar")
  lazy val dependenciesBucketItem: S3BucketItem = S3BucketItem(s3Bucket, "dependencies", "dependencies.zip", "dependencies.zip", "dependencies.jar")
  lazy val dependenciesLambdaLayer: LambdaLayerVersion = LambdaLayerVersion("testing", dependenciesBucketItem)

  def asLambdaFunction(scalambdaFunction: ProjectFunction): LambdaFunction = {
    LambdaFunction(scalambdaFunction, "1337", s3Bucket, sourcesBucketItem, dependenciesLambdaLayer)
  }
}
