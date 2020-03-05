package io.carpe.scalambda.terraform.ast.props

sealed trait TValue {
  def serialize: String
}

object TValue {
  case class TInt(int: Int) extends TValue {
    override def serialize: String = int.toString
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

  case class TResourceRef(resourceType: String, name: String, property: String) extends TValue {
    override def serialize: String = s"${resourceType}.${name}.${property}"
  }

}
