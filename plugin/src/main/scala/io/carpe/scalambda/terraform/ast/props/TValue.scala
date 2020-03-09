package io.carpe.scalambda.terraform.ast.props

sealed trait TValue {
  def serialize(implicit level: Int): String
}

object TValue {

  case class TNumber(number: Long) extends TValue {
    override def serialize(implicit level: Int): String = number.toString
  }

  case class TString(string: String) extends TValue {
    override def serialize(implicit level: Int): String = s""""${string.toString}""""
  }

  case class TBool(boolean: Boolean) extends TValue {
    override def serialize(implicit level: Int): String = if (boolean) {
      "true"
    } else {
      "false"
    }
  }


  abstract class TNested(props: (String, TValue)*) {
    val indent = "  "
    def serializeNested(implicit level: Int): String = {
      props.flatMap({ case (propertyName, propertyValue) =>
        val indentation = indent * level
        propertyValue match {
          case _: TValue.TBlock =>
            val innerBlock = propertyValue.serialize(level + 1)
            if (innerBlock.trim.nonEmpty) {
              Some(s"""${indentation}${propertyName} {
                 |$innerBlock
                 |${indentation}}""".stripMargin)
            } else {
              None
            }


          case _: TValue.TObject =>
            val innerBlock = propertyValue.serialize(level + 1)
            if (innerBlock.trim.nonEmpty) {
              Some(s"""$indentation$propertyName = {
                 |${innerBlock}
                 |$indentation}""".stripMargin)
            } else {
              None
            }

          case _ =>
            Some(s"$indentation$propertyName = ${propertyValue.serialize}")
        }
      }).mkString("\n")
    }
  }

  case class TBlock(props: (String, TValue)*) extends TNested(props: _*) with TValue {
    override def serialize(implicit level: Int): String = serializeNested(level)
  }

  case class TObject(props: (String, TValue)*) extends TNested(props: _*) with TValue {
    override def serialize(implicit level: Int): String = serializeNested(level)
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Data]]
   *
   * @param resourceType type or resource (i.e. "aws_iam_role")
   * @param name name of the resource (i.e. "my_personal_iam_role")
   * @param property name of the property on the resource type that is being referred to
   */
  case class TDataRef(resourceType: String, name: String, property: String) extends TValue {
    override def serialize(implicit level: Int): String = s"data.${resourceType}.${name}.${property}"
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Resource]]
   *
   * @param resourceType type or resource (i.e. "aws_iam_role")
   * @param name name of the resource (i.e. "my_personal_iam_role")
   * @param property name of the property on the resource type that is being referred to
   */
  case class TResourceRef(resourceType: String, name: String, property: String) extends TValue {
    override def serialize(implicit level: Int): String = s"${resourceType}.${name}.${property}"
  }

  /**
   * Reference to a defined "variable"
   * @param name of the referenced variable
   */
  case class TVariableRef(name: String) extends TValue {
    override def serialize(implicit level: Int): String = s"var.${name}"
  }

  /**
   * Used for edge cases such as the `type` property on a terraform `variable`
   * @param literal type
   */
  case class TLiteral(literal: String) extends TValue {
    override def serialize(implicit level: Int): String = literal
  }

}
