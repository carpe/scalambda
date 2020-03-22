package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBlock, TBool, TLiteral, TNone, TNumber, TString}

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

    val serializedBody = TBlock(body.toSeq: _*).serialize(level = 0)

    s"""${serializedHeader} ${serializedBody}
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

    /**
     * Examples: "aws_lambda_function" "template_file"
     * @return
     */
    def dataType: String
    override def resourceType: Option[String] = Some(dataType)
  }

  import scala.reflect.runtime.universe._

  case class Variable[T <: TValue](name: String, description: Option[String], defaultValue: Option[T])(implicit tag: TypeTag[T]) extends Definition {
    override def definitionType: String = "variable"
    override def resourceType: Option[String] = None

    /**
     * Properties of the definition
     */
    override def body: Map[String, TValue] = Map(
      "type" -> {

        typeOf[T] match {
          case t if t =:= typeOf[TNumber] => Some(TLiteral("string"))
          case t if t =:= typeOf[TNumber] => Some(TLiteral("number"))
          case _ => None
        }
      },
      "description" -> description.map(TString)
    ).collect {
      case (k, Some(v)) => k -> v
    }
  }

  case class Output[T <: TValue](name: String, description: Option[String], isSensitive: Boolean, value: T) extends Definition {
    /**
     * Examples: "data", "resource", "module"
     */
    override def definitionType: String = "output"

    /**
     * Examples: "aws_lambda_function" "aws_iam_role"
     */
    override def resourceType: Option[String] = None

    /**
     * Properties of the definition
     */
    override def body: Map[String, TValue] = Map(
      "value" -> value,
      "description" -> description.map(TString).getOrElse(TNone),
      "sensitive" -> TBool(isSensitive)
    )
  }
}