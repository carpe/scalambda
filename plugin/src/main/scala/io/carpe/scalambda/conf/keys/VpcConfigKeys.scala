package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.VpcConf
import io.carpe.scalambda.conf.function.VpcConf.StaticVpcConf

trait VpcConfigKeys {

  @deprecated("use `StaticVpcConfig` instead of `vpcConfig`", since = "5.1.0")
  def vpcConfig(subnetIds: Seq[String], securityGroupIds: Seq[String]): VpcConf = StaticVpcConf(subnetIds, securityGroupIds)


  /**
   * Use this option if you'd like to force a static VPC config. Not recommended! It is much better to use [[VpcFromTF]]!
   *
   * @return the vpc configuration
   */
  def StaticVpcConfig(subnetIds: Seq[String], securityGroupIds: Seq[String]): VpcConf = VpcConf.StaticVpcConf(subnetIds, securityGroupIds)

  /**
   * Use this option if you'd like to fetch the vpc configuration based on inputs to the generated terraform module.
   *
   * If you select this option, variables will be generated in the terraform module produced by the scalambdaTerraform command.
   *
   * @return the vpc configuration
   */
  def VpcFromTF: VpcConf = VpcConf.VpcFromTF

}
