package io.carpe.scalambda.conf.function

sealed trait VpcConf

object VpcConf {

  case class StaticVpcConf(subnetIds: Seq[String], securityGroupIds: Seq[String]) extends VpcConf

  case object VpcFromTF extends VpcConf

  lazy val withoutVpc: VpcConf = StaticVpcConf(Seq.empty, Seq.empty)
}
