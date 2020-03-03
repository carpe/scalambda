package io.carpe.scalambda.terraform.ast

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}

case class TerraformFile(definitions: Seq[Definition], fileName: String) {
  override def toString: String = definitions.map(_.toString).mkString("\n")
}

object TerraformFile {

  def writeFile(file: TerraformFile, rootPath: String): Unit = {
    val filePath = rootPath + file.fileName
    val fileContent = file.toString

    new PrintWriter(filePath) { write(fileContent); close() }
  }

  def writeFiles(files: Seq[TerraformFile], rootPath: String): Unit = {
    Files.createDirectories(new File(rootPath).toPath)

    files.foreach(file => {
      writeFile(file, rootPath)
    })
  }
}
