package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.terraform.ast.props.TValue.{TIf, TNumber, TVariableRef}
import io.carpe.scalambda.terraform.writer.TerraformPrinter
import org.scalatest.flatspec.AnyFlatSpec

class DefinitionSpec extends AnyFlatSpec {

  "TIf" should "be able to be serialized into proper HCL" in {
    val node = TIf(TVariableRef("some_var"), ifTrue = TNumber(1), ifFalse = TNumber(2))
    val expectation = """var.some_var ? 1 : 2"""

    val actual = TerraformPrinter.print(node.serialize(0))

    assert(actual === expectation)

  }

}
