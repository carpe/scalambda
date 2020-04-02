package io.carpe.scalambda.terraform.ast.props

import cats.data.Chain

/**
 * A line of Terraform code
 */
sealed trait TLine {
  final def appendOnto(builder: StringBuilder): StringBuilder = {
    this match {
      case TLine.TInline(contents) =>
        builder.append(contents)
      case TLine.TBlockLine(indentationLevel, contents) =>
        if (builder.nonEmpty) {
          // appending newline if this is not the only content in the string
          builder.append("\n")
        }

        // append indentation
        for (i <- 0 until indentationLevel) builder append TLine.indentation

        // append contents
        builder.append(contents)
      case TLine.TEmptyLine =>
        builder.append("\n")
    }
  }
}

object TLine {
  lazy val indentation: String = "  "

  case class TInline(contents: String) extends TLine

  case class TBlockLine(indentationLevel: Int, contents: String) extends TLine

  case object TEmptyLine extends TLine

  def apply(contents: String)(implicit level: Int): Chain[TLine] = {
    Chain.one(
      TInline(contents)
    )
  }
}
