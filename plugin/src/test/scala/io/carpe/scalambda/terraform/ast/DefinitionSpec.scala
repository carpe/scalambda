package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue._
import io.carpe.scalambda.terraform.ast.props.{TDynamicBlock, TValue}
import io.carpe.scalambda.terraform.writer.TerraformPrinter
import org.scalatest.flatspec.AnyFlatSpec

class DefinitionSpec extends AnyFlatSpec {

  "TIf" should "be able to be serialized into proper HCL" in {
    val node = TIf(TVariableRef("some_var"), ifTrue = TNumber(1), ifFalse = TNumber(2))
    val expectation = """var.some_var ? 1 : 2"""

    val actual = TerraformPrinter.print(node.serialize(0))

    assert(actual === expectation)

  }

  "Resource with DynamicBlock" should "be able to be serialized into proper HCL" in {
    object TestTResource extends Resource {
      override def resourceType: String = "dynamic_duo"

      override def name: String = "wolf"

      override def body: Map[String, TValue] = Map(
        "has_kusabimaru" -> TBool(true)
      )

      override def dynamicBlocks: Seq[TDynamicBlock] = Seq({
        val forEachPredicate = TIf(TVariableRef("has_flame_barrel"), ifTrue = TLiteral("toset([1])"), ifFalse = TLiteral("toset([])"))
        val properties = Map(
          "good_against" -> TArray(TString("beasts"), TString("chained_ogre")),
          "spirit_emblem_cost" -> TNumber(2)
        )
        TDynamicBlock("flame_vent", forEach = forEachPredicate, props = properties)
      })
    }

    val actual: String = TerraformPrinter.print(TestTResource.serialize)

    val expected = """resource "dynamic_duo" "wolf" {
                     |  has_kusabimaru = true
                     |  dynamic "flame_vent" {
                     |    for_each = var.has_flame_barrel ? toset([1]) : toset([])
                     |    content {
                     |      good_against = [
                     |        "beasts",
                     |        "chained_ogre"
                     |      ]
                     |      spirit_emblem_cost = 2
                     |    }
                     |  }
                     |}""".stripMargin

    assert(actual === expected)
  }

}
