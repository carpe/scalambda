package io.carpe.scalambda.terraform.ast.props

import cats.data.Chain
import io.carpe.scalambda.terraform.ast.Definition.{Data, Resource}
import io.carpe.scalambda.terraform.ast.props.TLine.{TBlockLine, TEmptyLine, TInline}

/**
 * Represents a node in the Terraform AST. Specifically a value that is assigned to some definition.
 *
 * A Node can be composed into a [[TLine]], which can then be used by a
 * [[io.carpe.scalambda.terraform.writer.TerraformPrinter]] to write a String representation of the AST to a file.
 *
 * @param usesAssignment whether or not the node should be assigned with an `=`. Blocks, for example, cannot be assigned.
 */
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

  case class TIf(predicate: TValue, ifTrue: TValue, ifFalse: TValue) extends TValue {
    override def serialize(implicit level: Int): Chain[TLine] = TLine(s"${predicate.serialize} ? ${ifTrue.serialize} : ${ifFalse.serialize}")
  }

  case class TString(string: String) extends TValue {
    lazy val escapedString: String = {
      import io.circe.syntax._
      string.asJson.noSpaces
    }

    override def serialize(implicit level: Int): Chain[TLine] = {
      TLine(escapedString)
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

  case class TArray[A <: TValue](values: A*) extends TValue {
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
     * @example data.aws_lambda_function.my_function.name
     * @return String that can be used for interpolating strings.
     */
    def asInterpolatedRef: String

    override def serialize(implicit level: Int): Chain[TLine] = TLine(asInterpolatedRef)
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Data]]
   *
   * @param data that is being referenced
   * @param property name of the property on the data resource type that is being referred to
   */
  case class TDataRef(data: Data, property: String) extends TRef {
    type A = TDataRef

    override def asInterpolatedRef: String = s"data.${data.dataType}.${data.name}.${property}"
  }

  /**
   * Reference to property on a [[io.carpe.scalambda.terraform.ast.Definition.Resource]]
   *
   * @param resource the resource that is being referred to
   * @param property name of the property on the resource that is being referred to
   */
  case class TResourceRef(resource: Resource, property: String) extends TRef {
    type A = TResourceRef
    override def asInterpolatedRef: String = s"${resource.resourceType}.${resource.name}.${property}"
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
