package io.carpe.scalambda.fixtures

import io.carpe.scalambda.terraform.ast.{Definition, TerraformFile}
import io.carpe.scalambda.terraform.writer.TerraformPrinter
import org.scalatest.flatspec.AnyFlatSpec

trait TerraformBehaviors { this: AnyFlatSpec =>

  def printableTerraform(input: TerraformFile, expected: String): Unit = {
    it should "be able to be printed by the Printer" in {
      val actual = TerraformPrinter.print(input)
      assert(actual == expected)
    }
  }

  def printableTerraform(input: Definition, expected: String): Unit = {
    it should "be able to be printed by the Printer" in {
      val actual = TerraformPrinter.print(input.serialize)
      assert(actual == expected)
    }
  }
}
