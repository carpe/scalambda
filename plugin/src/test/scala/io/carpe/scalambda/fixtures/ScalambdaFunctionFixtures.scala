package io.carpe.scalambda.fixtures

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.ScalambdaFunction.DefinedFunction
import io.carpe.scalambda.conf.api.ApiGatewayEndpoint
import io.carpe.scalambda.conf.function.FunctionNaming.Static
import io.carpe.scalambda.conf.function.FunctionSource.IncludedInModule
import io.carpe.scalambda.conf.function._
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.{LambdaFunction, LambdaLayerVersion}
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}
import org.scalatest.flatspec.AnyFlatSpec

trait ScalambdaFunctionFixtures { this: AnyFlatSpec =>

  lazy val carsIndexFunction: ScalambdaFunction.DefinedFunction = {
    ScalambdaFunction.DefinedFunction(
      Static("CarsIndex"), "io.cars.index.CarsIndex::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.RoleFromArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      runtimeConfig = RuntimeConfig.default,
      vpcConfig = VpcConf.withoutVpc,
      warmerConfig = WarmerConfig.Cold,
      environmentVariables = List.empty
    )
  }

  lazy val carsIndexEndpoint: ApiGatewayEndpoint = ApiGatewayEndpoint(url = "/cars", method = Method.GET, auth = Auth.TokenAuthorizer("my_authorizer"))

  lazy val driveCarFunction: ScalambdaFunction.DefinedFunction = {
    ScalambdaFunction.DefinedFunction(
      Static("DriveCar"), "io.cars.lambda.DriveCar::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.RoleFromArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      runtimeConfig = RuntimeConfig.default,
      vpcConfig = VpcConf.withoutVpc,
      warmerConfig = WarmerConfig.Cold,
      environmentVariables = List(
        EnvironmentVariable.StaticVariable("API", "www.google.com")
      )
    )
  }

  lazy val flyPlaneFunction: ScalambdaFunction.DefinedFunction = {
    ScalambdaFunction.DefinedFunction(
      Static("FlyPlane"), "io.plane.lambda.FlyPlane::handler",
      functionSource = IncludedInModule,
      iamRole = FunctionRoleSource.RoleFromArn("arn:aws:iam::12345678900:role/lambda_basic_execution"),
      runtimeConfig = RuntimeConfig.default.copy(memory = 256, timeout = 30),
      vpcConfig = VpcConf.StaticVpcConf(
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

  def asLambdaFunction(scalambdaFunction: DefinedFunction): LambdaFunction = {
    LambdaFunction(scalambdaFunction, subnetIds = TArray(TString("abc123")), securityGroupIds = TArray(TString("def456")), "1337", s3Bucket, sourcesBucketItem, dependenciesLambdaLayer, isXrayEnabled = false, billingTags = Nil)
  }
}
