package io.carpe.scalambda.terraform.ast.data

import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TVariableRef}

case object TemplateFile extends Data {

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
  override def body: Map[String, TValue] = Map(
    "template" -> TLiteral("file(\"${path.module}/swagger.yaml\")"),
    "vars" -> TLiteral("merge({\"api_name\" = local.api_name }, {\"verifier_invoke_arn\" = var.lambda_verifier.invoke_arn }, { for k, v in var.lambdas : k => v.invoke_arn })")
  )
}
