package io.carpe.scalambda.conf.function

case class VpcConf(subnetIds: Seq[String], securityGroupIds: Seq[String])

object VpcConf {

  def vpcConfig(subnetIds: Seq[String], securityGroupIds: Seq[String]): VpcConf = VpcConf(subnetIds, securityGroupIds)

  lazy val withoutVpc: VpcConf = VpcConf(Seq.empty, Seq.empty)
}
