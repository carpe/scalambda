package io.carpe.scalambda.conf.keys

import sbt._
import _root_.io.carpe.scalambda.conf.ScalambdaFunction

trait ScalambaKeys extends FunctionNamingKeys
  with FunctionRoleSourceKeys
  with VpcConfigKeys
  with ApiGatewayKeys
  with RuntimeKeys
  with WarmerKeys
  with BillingTagKeys
  with AssemblyKeys
  with XRayKeys
  with TerraformKeys {

  lazy val scalambdaFunctions = settingKey[Seq[ScalambdaFunction]]("List of Scalambda Functions")

  lazy val s3BucketName = settingKey[String]("Prefix for S3 bucket name to store lambda functions in. Defaults to project name.")
}
