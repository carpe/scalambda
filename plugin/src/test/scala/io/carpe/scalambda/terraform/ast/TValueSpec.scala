package io.carpe.scalambda.terraform.ast

import io.carpe.scalambda.terraform.ast.props.TValue.{InfixExpression, TNumber, TString}
import io.carpe.scalambda.terraform.writer.TerraformPrinter
import org.scalatest.flatspec.AnyFlatSpec

class TValueSpec extends AnyFlatSpec {

  "InfixExpression" should "be serializable" in {
    val testExpression = InfixExpression(TNumber(1), "==", TNumber(2))

    val actual = TerraformPrinter.print(testExpression.serialize(0))

    val expected = "1 == 2"

    assert(actual === expected)
  }
}
