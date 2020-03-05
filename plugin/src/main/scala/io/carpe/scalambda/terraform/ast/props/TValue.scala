package io.carpe.scalambda.terraform.ast.props

sealed trait TValue {
  def serialize: String
}

object TValue {
  case class TNumber(number: Long) extends TValue {
    override def serialize: String = number.toString
  }


  case class TString(string: String) extends TValue {
    override def serialize: String = s""""${string.toString}""""
  }

  case class TBool(boolean: Boolean) extends TValue {
    override def serialize: String = if (boolean) {
      "true"
    } else {
      "false"
    }
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Resource]]
   *
   * @param resourceType type or resource (i.e. "aws_iam_role")
   * @param name name of the resource (i.e. "my_personal_iam_role")
   * @param property name of the property on the resource type that is being referred to
   */
  case class TResourceRef(resourceType: String, name: String, property: String) extends TValue {
    override def serialize: String = s"${resourceType}.${name}.${property}"
  }

  /**
   * Reference to a defined "variable"
   * @param name of the referenced variable
   */
  case class TVariableRef(name: String) extends TValue {
    override def serialize: String = s"var.${name}"
  }

  /**
   * Used for edge cases such as the `type` property on a terraform `variable`
   * @param literal type
   */
  case class TLiteral(literal: String) extends TValue {
    override def serialize: String = literal
  }

}
