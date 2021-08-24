package io.carpe.scalambda.terraform.ast.props

import cats.data.Chain
import io.carpe.scalambda.terraform.ast.props.TLine.TBlockLine

/**
 * Node that represents an Dynamic Block. Reference: https://www.terraform.io/docs/language/expressions/dynamic-blocks.html.
 */
case class TDynamicBlock(settingName: String, forEach: TValue, props: Map[String, TValue])

object TDynamicBlock {

  def serializeDynamicBlock(dynamicBlock: TDynamicBlock)(implicit level: Int): Chain[TLine] = {
    // dynamic ??? {
    TBlockLine(level, s"""dynamic "${dynamicBlock.settingName}" {""") +: ({
      // for_each = ???
      TBlockLine(indentationLevel = level + 1, contents = "for_each = ") +: dynamicBlock.forEach.serialize
    } ++ {
      // content {
      TBlockLine(indentationLevel = level + 1, contents = "content {") +: {
        serializeProperties(dynamicBlock.props)(level + 2)
      } :+ TBlockLine(indentationLevel = level + 1, contents = "}")
      // }
    }) :+ TBlockLine(level, "}")
    // }
  }

  private def serializeProperties(properties: Map[String, TValue])(implicit level: Int): Chain[TLine] = {
    val propertiesChain = Chain.fromSeq(properties.toSeq)

    propertiesChain.flatMap({ case (propertyName: String, propertyValue: TValue) =>

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
