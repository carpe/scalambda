package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.{ApiFunction, ProjectFunction}
import io.carpe.scalambda.conf.api.ApiGatewayConfig
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.{LambdaFunction, LambdaLayerVersion}
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>
  lazy val carsIndexFunction: ApiFunction = {
    ScalambdaFunction.ApiFunction(
      Static("CarsIndex"), "io.cars.index.CarsIndex::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      runtimeConfig = RuntimeConfig.default,
      apiConfig = ApiGatewayConfig(route = "/cars", method = Method.GET, authConf = Auth.Authorizer("my_authorizer", "arn:fake:my_authorizer/invocations", "arn:fake:MyRole")),
      vpcConfig = VpcConf.withoutVpc,
      warmerConfig = WarmerConfig.Cold,
      environmentVariables = List.empty
    )
  }

  lazy val driveCarFunction: ScalambdaFunction.Function = {
    ScalambdaFunction.Function(
      Static("DriveCar"), "io.cars.lambda.DriveCar::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.StaticArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      runtimeConfig = RuntimeConfig.default,
      vpcConfig = VpcConf.withoutVpc,
      warmerConfig = WarmerConfig.Cold,
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
      runtimeConfig = RuntimeConfig.default.copy(memory = 256, timeout = 30),
      vpcConfig = VpcConf(
        subnetIds = Seq("subnet-12345678987654321"),
        securityGroupIds = Seq("sg-12345678987654321")
      ),
      warmerConfig = WarmerConfig.Cold,
      environmentVariables = List.empty
    )
  }

  lazy val s3Bucket: S3Bucket = S3Bucket("testing", billingTags = Nil)
  lazy val sourcesBucketItem: S3BucketItem = S3BucketItem(s3Bucket, "sources", "sources.jar", "sources.jar", TString("sources.jar"), billingTags = Nil)
  lazy val dependenciesBucketItem: S3BucketItem = S3BucketItem(s3Bucket, "dependencies", "dependencies.zip", "dependencies.zip", TString("dependencies.jar"), billingTags = Nil)
  lazy val dependenciesLambdaLayer: LambdaLayerVersion = LambdaLayerVersion("testing", dependenciesBucketItem)

  def asLambdaFunction(scalambdaFunction: ProjectFunction): LambdaFunction = {
    LambdaFunction(scalambdaFunction, "1337", s3Bucket, sourcesBucketItem, dependenciesLambdaLayer, isXrayEnabled = false, billingTags = Nil)
  }
}
