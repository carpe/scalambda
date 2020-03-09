package io.carpe.scalambda.terraform.ast.data

import java.io.File

import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TString

case class ArchiveFile(name: String, source: String, output: String) extends Data {

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
    "source_file" -> TString("${path.module}/" + source),
    "output_path" -> TString("${path.module}/" + output)
  )
}


//data "archive_file" "init" {
//  type        = "zip"
//  source_file = "${path.module}/init.tpl"
//  output_path = "${path.module}/files/init.zip"
//}