package io.carpe.scalambda.terraform.ast.props

import cats.data.Chain
import io.carpe.scalambda.terraform.ast.Definition.{Data, Resource}
import io.carpe.scalambda.terraform.ast.props.TLine.{TBlockLine, TEmptyLine, TInline}

sealed abstract class TValue(val usesAssignment: Boolean = true) {
  def serialize(implicit level: Int): Chain[TLine]
}

object TValue {

  /**
   * Primitives
   */

  case class TNumber(number: Long) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = TLine(number.toString)
  }

  case class TString(string: String) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = {
      TLine(s""""${string.toString}"""")
    }
  }

  case class TBool(boolean: Boolean) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = if (boolean) {
      TLine("true")
    } else {
      TLine("false")
    }
  }


  /**
   * Used for String literals.
   *
   * For example:
   * <<EOF
   * hello
   * world
   * EOF
   *
   * @see [[https://www.terraform.io/docs/configuration/expressions.html#string-literals Terraform Docs]]
   * @param fileContents to place inside the heredoc
   */
  case class THeredoc(fileContents: String) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = {
      if (fileContents.isEmpty) {
        return Chain.empty
      }

      TInline("<<EOF") +: Chain.fromSeq(
        fileContents.lines.toSeq
          .map(line => TBlockLine(indentationLevel = 0, contents = line))
      ) :+ TBlockLine(0, "EOF")
    }
  }

  /**
   * Objects
   */

  trait TProperties { this: TValue =>
    def props: Seq[(String, TValue)]
    lazy val propChain: Chain[(String, TValue)] = Chain.fromSeq(props)


    override def serialize(implicit level: Int): Chain[TLine] = {
      val serializedProperties = serializeProperties(level + 1)

      if (serializedProperties.isEmpty) {
        return Chain.empty
      }

      TInline("{") +: serializedProperties :+ TBlockLine(level, "}")
    }

    def serializeProperties(implicit level: Int): Chain[TLine] = {
      propChain.flatMap({ case (propertyName, propertyValue: TValue) =>

        // serialize the inner properties
        val propertyChain = propertyValue.serialize

        // if the inner properties are blank, remove this key altogether
        if (propertyChain.isEmpty) {
          Chain.empty[TLine]
        } else {
          val assignmentType = if (propertyValue.usesAssignment) " = " else " "
          val assigment = TBlockLine(level, s"$propertyName$assignmentType")
          assigment +: propertyChain
        }
      })
    }
  }

  case class TBlock(props: (String, TValue)*) extends TValue(usesAssignment = false) with TProperties

  object TBlock {
    def optionally(props: (String, Option[TValue])*): TBlock = {
      new TBlock(props.toSeq.flatMap { case (key, maybeValue) => {
        maybeValue.map(key -> _)
      } }: _*)
    }
  }

  case class TObject(props: (String, TValue)*) extends TValue(usesAssignment = true) with TProperties

  /**
   * Collections
   */

  case class TArray(values: TValue*) extends TValue {
    lazy val valueChain: Chain[TValue] = Chain.fromSeq(values)

    override def serialize(implicit level: Int): Chain[TLine] = {
      val innerValueChains = valueChain.map(value => {
        val innerArrayLevel = level + 1
        val chain = value.serialize(innerArrayLevel)

        chain.uncons.map {
          case (TInline(contents), tail: Chain[TLine]) =>
            Chain.concat(
              Chain.one(TBlockLine(innerArrayLevel, contents)),
              tail
            )
          case (TBlockLine(indentationLevel, contents), tail: Chain[TLine]) =>
            chain
          case (TEmptyLine, _) =>
            chain
        }.getOrElse(Chain.empty)
      })

      innerValueChains.initLast.map({ case (initChains, lastChain) =>
        // append a comma to each element except the last
        (initChains.flatMap(innerValueChain => innerValueChain :+ TInline(",\n"))) ++ lastChain
      }).map(innerValuesChain => {
        // add array brackets
        TInline("[") +: innerValuesChain :+ TBlockLine(level, "]")
      }).getOrElse(Chain.empty)
    }
  }

  /**
   * References
   */


  sealed trait TRef extends TValue {
    type A <: TRef

    /**
     * Use this to override references to point to a different property.
     * @param property to change the reference to
     * @return
     */
    def overrideProperty(property: String): A
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Data]]
   *
   * @param data that is being referenced
   * @param property name of the property on the data resource type that is being referred to
   */
  case class TDataRef(data: Data, property: String) extends TRef {
    type A = TDataRef
    override def serialize(implicit level: Int): Chain[TLine] = TLine(s"data.${data.dataType}.${data.name}.${property}")
    override def overrideProperty(property: String): TDataRef = this.copy(property = property)
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Resource]]
   *
   * @param resource the resource that is being referred to
   * @param property name of the property on the resource that is being referred to
   */
  case class TResourceRef(resource: Resource, property: String) extends TRef {
    type A = TResourceRef
    override def serialize(implicit level: Int): Chain[TLine] = TLine(s"${resource.resourceType}.${resource.name}.${property}")
    override def overrideProperty(property: String): TResourceRef = this.copy(property = property)
  }

  /**
   * Reference to a defined "variable"
   * @param name of the referenced variable
   */
  case class TVariableRef(name: String) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = TLine(s"var.${name}")
  }

  /**
   * Misc
   */

  /**
   * Used for edge cases such as the `type` property on a terraform `variable`
   * @param literal type
   */
  case class TLiteral(literal: String) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = TLine(literal)
  }

  case object TNone extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = Chain.empty
  }

}
