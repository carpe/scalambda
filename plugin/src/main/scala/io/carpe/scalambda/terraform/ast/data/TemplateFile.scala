package io.carpe.scalambda.terraform.ast.data

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition.Data
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TObject, TResourceRef, TString}

case class TemplateFile(filename: String, apiName: String, lambdas: Seq[ScalambdaFunction]) extends Data {

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
    val lambdaVars: Seq[(String, TValue)] = lambdas.flatMap(lambda => {
      lambda match {
        case function: ScalambdaFunction.ProjectFunction =>
          Some(function.swaggerVariableName -> TResourceRef("aws_lambda_function", function.terraformLambdaResourceName, "invoke_arn"))
        case ScalambdaFunction.ReferencedFunction(functionName, qualifier, functionArn, apiGatewayConf) =>
          None
      }
    })

    Map(
      "template" -> TLiteral("file(\"${path.module}/swagger.yaml\")"),
      "vars" -> TObject(
        Seq(
          "api_name" -> TString(apiName)
        ) ++ lambdaVars: _*
      )
    )
  }
}
