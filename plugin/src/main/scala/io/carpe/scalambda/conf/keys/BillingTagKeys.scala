package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import sbt.settingKey

trait BillingTagKeys {
  lazy val billingTags = settingKey[Seq[BillingTag]]("AWS Billing Tags to apply to all terraformed resources.")

  def BillingTag(name: String, value: String): BillingTag = new BillingTag(name, value)
}
