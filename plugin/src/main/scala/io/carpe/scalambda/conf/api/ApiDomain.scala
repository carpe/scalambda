package io.carpe.scalambda.conf.api

import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TString, TVariableRef}

sealed trait ApiDomain {
  def map[R](f: TValue => R): Option[R]
}

object ApiDomain {

  /**
   * Refuse to map the domain name to any thing
   */
  case object Unmapped extends ApiDomain {
    override def map[R](f: TValue => R): Option[R] = None
  }

  /**
   * The Api's domain name will be retrieved from a generated terraform variable.
   */
  case object FromVariable extends ApiDomain {
    lazy val apiDomainVariableName: String = "domain_name"
    lazy val asTValue: TValue = TVariableRef(apiDomainVariableName)

    override def map[R](f: TValue => R): Option[R] = Some(f.apply(this.asTValue))
  }

  /**
   * Sets the deployment/stage of the Api as the root Custom Domain Mapping for the given url.
   *
   * @param domain to connect api to as the root mapping
   */
  case class Static(domain: String) extends ApiDomain {
    lazy val asTValue: TValue = TString(domain)

    override def map[R](f: TValue => R): Option[R] = Some(f.apply(this.asTValue))
  }
}
