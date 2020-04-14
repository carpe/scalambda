package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.VpcConf

trait VpcConfigKeys {

  def vpcConfig(subnetIds: Seq[String], securityGroupIds: Seq[String]): VpcConf = VpcConf(subnetIds, securityGroupIds)

}
