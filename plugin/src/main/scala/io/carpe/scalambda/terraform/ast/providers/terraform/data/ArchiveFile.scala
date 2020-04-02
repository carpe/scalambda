package io.carpe.scalambda.terraform.ast.providers.terraform.data

import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.providers.terraform.data.ArchiveFile.ArchiveSource

case class ArchiveFile(name: String, source: ArchiveSource, output: String) extends Data {

  /**
   * Examples: "aws_lambda_function" "template_file"
   *
   * @return
   */
  override def dataType: String = "archive_file"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "type" -> TString("zip"),
    source.key -> source.value,
    "output_path" -> TString("${path.module}/" + output)
  )
}

object ArchiveFile {
  sealed trait ArchiveSource {
    def key: String
    def value: TValue
  }

  object ArchiveSource {

    /**
     * A single file to create an archive for
     * @param path relative to inside of module. (e.g. "source.jar", "my/file.txt")
     */
    case class SourceFile(path: String) extends ArchiveSource {
      override def key: String = "source_file"
      override def value: TValue = TString("${path.module}/" + path)
    }

    /**
     * A folder to create an archive for
     * @param path relative to inside of module. (e.g. "sources", "my/folder")
     */
    case class SourceFolder(path: String) extends ArchiveSource {
      override def key: String = "source_dir"
      override def value: TValue = TString("${path.module}/" + path)
    }
  }
}