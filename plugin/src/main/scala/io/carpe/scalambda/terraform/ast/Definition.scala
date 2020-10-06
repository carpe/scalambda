package io.carpe.scalambda.terraform.ast

import cats.data.Chain
import io.carpe.scalambda.terraform.ast.props.TLine.TBlockLine
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.props.{TLine, TValue}

/**
 * A single piece of HCL configuration. Such as a [[io.carpe.scalambda.terraform.ast.Definition.Resource]].
 */
trait Definition {

  /**
   * Examples: "data", "resource", "module"
   */
  def definitionType: String

  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  protected [ast] def getResourceType: Option[String]

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   * @return
   */
  def name: String


  /**
   * Properties of the definition
   */
  def body: Map[String, TValue]

  def serialize: Chain[TLine] = {
    val headerContent = Seq(Some(definitionType), getResourceType.map(r => s""""$r""""), Some(s""""$name"""")).flatten.mkString(" ")

    val bodyChain = TBlock(body.toSeq: _*).serialize(level = 0)

    TBlockLine(0, headerContent + " ") +: bodyChain
  }
}

object Definition {

  /**
   * A Terraform resource definition
   */
  abstract class Resource extends Definition {
    override def definitionType: String = "resource"
    override final lazy val getResourceType: Option[String] = Some(resourceType)

    /**
     * Examples: "aws_lambda_function" "aws_iam_role"
     *
     * Can be null in the case of terraform modules!
     */
    def resourceType: String
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
    override def getResourceType: Option[String] = Some(dataType)
  }

  import scala.reflect.runtime.universe._

  case class Variable[T <: TValue](name: String, description: Option[String], defaultValue: Option[T])(implicit tag: TypeTag[T]) extends Definition {
    override def definitionType: String = "variable"

    override def getResourceType: Option[String] = None

    def ref: TVariableRef = TVariableRef(name)

    /**
     * Properties of the definition
     */
    override def body: Map[String, TValue] = Map(
      "type" -> {
        typeOf[T] match {
          case t if t =:= typeOf[TString] => Some(TLiteral("string"))
          case t if t =:= typeOf[TNumber] => Some(TLiteral("number"))
          case t if t =:= typeOf[TBool] => Some(TLiteral("bool"))
          case t if t =:= typeOf[TObject] => Some(TLiteral("map(any)"))
          case _ => None
        }
      },
      "description" -> description.map(TString),
      "default" -> defaultValue
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
    override def getResourceType: Option[String] = None

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