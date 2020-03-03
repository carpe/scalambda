package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.terraform.ast.props.TValue

/**
 * A single piece of HCL configuration. Such as a [[io.carpe.scalambda.terraform.ast.Definition.Resource]].
 */
sealed trait Definition {

  /**
   * Examples: "data", "resource", "module"
   */
  def definitionType: String

  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  def resourceType: Option[String]

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   * @return
   */
  def name: String


  /**
   * Properties of the definition
   */
  def body: Map[String, TValue]


  override def toString: String = {
    val serializedHeader = Seq(Some(definitionType), resourceType.map(r => s""""$r""""), Some(s""""$name"""")).flatten.mkString(" ")

    val indent = "  "
    val serializedBody = body.map({ case (propertyName, propertyValue) =>
      s"$indent$propertyName = ${propertyValue.serialize}"
    }).mkString("\n")


    s"""${serializedHeader} {
      |${serializedBody}
      |}
      |""".stripMargin
  }
}

object Definition {

  /**
   * A Terraform resource definition
   */
  abstract class Resource extends Definition {
    override def definitionType: String = "resource"
  }

  /**
   * A Terraform data-type resource definition
   */
  abstract class Data extends Definition {
    override def definitionType: String = "data"
  }
}