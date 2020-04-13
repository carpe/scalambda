package io.carpe.scalambda.terraform.ast.providers.terraform.data

import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TObject, TString}

case class TemplateFile(filename: String, apiName: String, lambdaVars: Seq[(String, TValue)]) extends Data {

  /**
   * Examples: "aws_lambda_function" "template_file"
   *
   * @return
   */
  override def dataType: String = "template_file"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = "swagger_spec"

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = {
    Map(
      "template" -> TLiteral("file(\"${path.module}/swagger.yaml\")"),
      "vars" -> TObject(
        Seq(
          "api_name" -> TString(apiName + "-${terraform.workspace}")
        ) ++ lambdaVars: _*
      )
    )
  }
}
