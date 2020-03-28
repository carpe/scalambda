package io.carpe.scalambda.terraform.ast.data

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TObject, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.resources.lambda.LambdaFunctionAlias

case class TemplateFile(filename: String, apiName: String, aliases: Seq[LambdaFunctionAlias]) extends Data {

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
    val lambdaVars: Seq[(String, TValue)] = aliases.map(alias => {
      val function = alias.function.scalambdaFunction
      function.swaggerVariableName -> TResourceRef("aws_lambda_alias", function.terraformLambdaResourceName, "invoke_arn")
    })

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
