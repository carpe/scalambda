package io.carpe.scalambda.conf.function

case class VpcConf(subnetIds: Seq[String], securityGroupIds: Seq[String])

object VpcConf {

  lazy val withoutVpc: VpcConf = VpcConf(Seq.empty, Seq.empty)
}
