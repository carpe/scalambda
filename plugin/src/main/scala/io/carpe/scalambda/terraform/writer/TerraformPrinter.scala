package io.carpe.scalambda.terraform.writer

import java.io.{File, PrintWriter}
import java.nio.file.Files

import cats.data.Chain
import io.carpe.scalambda.terraform.ast.TerraformFile
import io.carpe.scalambda.terraform.ast.props.TLine
import io.carpe.scalambda.terraform.ast.props.TLine.TEmptyLine

object TerraformPrinter {

  def print(lines: Chain[TLine]): String = {
    lines.foldLeft(StringBuilder.newBuilder)((current, nextLine) => {
      nextLine.appendOnto(current)
    }).mkString
  }

  def print(file: TerraformFile): String = {
    val contentsChain = file.definitionsChain.flatMap(definition => {
      definition.serialize :+ TEmptyLine
    })

    val fullFileChain = TerraformFile.fileHeaderChain ++ contentsChain

    print(fullFileChain)
  }

  def writeFile(rootPath: String, file: TerraformFile): Unit = {
    if (file.definitions.isEmpty) {
      // refuse to write files that are empty
      return
    }

    Files.createDirectories(new File(rootPath).toPath)

    val filePath = rootPath + "/" + file.fileName
    val fileContent = print(file)

    new PrintWriter(filePath) { write(fileContent); close() }
  }
}
