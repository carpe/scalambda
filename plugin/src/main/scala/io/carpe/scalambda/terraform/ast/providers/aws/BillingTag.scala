package io.carpe.scalambda.terraform.ast.providers.aws

/**
 * AWS Billing Tag, used for tracking cost.
 *
 * Make sure to enable any Billing Tags you create in order to be able to track them.
 *
 * @param name of the tag
 * @param value of the tag
 */
case class BillingTag(name: String, value: String)
