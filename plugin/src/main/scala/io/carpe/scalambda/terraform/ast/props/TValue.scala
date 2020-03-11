package io.carpe.scalambda.terraform.ast.props

import io.carpe.scalambda.terraform.ast.Definition.Data

sealed abstract class TValue(val usesAssignment: Boolean = true) {
  def indent(implicit level: Int): String = ("  " * level)
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

  trait TProperties { this: TValue =>
    def props: Seq[(String, TValue)]


    override def serialize(implicit level: Int): String = {
      val serializedProperties = serializeProperties(level + 1)

      if (serializedProperties.trim.isEmpty) {
        return ""
      }

      s"""{
         |${serializedProperties}
         |$indent}""".stripMargin
    }

    def serializeProperties(implicit level: Int): String = {
      props.flatMap({ case (propertyName, propertyValue: TValue) =>

        // serialize the inner properties
        val innerBlock = propertyValue.serialize

        // if the inner properties are blank, remove this key altogether
        if (innerBlock.trim.isEmpty) {
          None
        } else {
          Some(s"""$indent$propertyName${if (propertyValue.usesAssignment) " = " else " "}$innerBlock""")
        }
      }).mkString("\n")
    }
  }

  case class TBlock(props: (String, TValue)*) extends TValue(usesAssignment = false) with TProperties

  case class TObject(props: (String, TValue)*) extends TValue(usesAssignment = true) with TProperties

  case class TArray(values: TValue*) extends TValue {
    override def serialize(implicit level: Int): String = {
      val serializedValues = values.map(value => {
        s"${indent(level + 1)}${value.serialize(level + 2)}"
      })

      if (values.isEmpty) {
        return ""
      }

      s"""[
        |${serializedValues.mkString(",\n")}
        |$indent]""".stripMargin
    }
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Data]]
   *
   * @param data that is being referenced
   * @param property name of the property on the data resource type that is being referred to
   */
  case class TDataRef(data: Data, property: String) extends TValue {
    override def serialize(implicit level: Int): String = s"data.${data.dataType}.${data.name}.${property}"
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
