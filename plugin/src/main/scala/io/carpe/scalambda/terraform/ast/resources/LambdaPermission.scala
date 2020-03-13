package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TNone, TResourceRef, TString}

case class LambdaPermission(lambda: ScalambdaFunction, apiGateway: ApiGateway) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   */
  override def resourceType: Option[String] = Some("aws_lambda_permission")

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = s"${lambda.terraformLambdaResourceName}_api_permission"

  val statementId: String = lambda match {
    case function: ScalambdaFunction.ProjectFunction =>
      "Allow${title(aws_lambda_function." + function.terraformLambdaResourceName + ".function_name)}InvokeBy${title(aws_api_gateway_rest_api." + apiGateway.name + ".name)}"
    case ScalambdaFunction.ReferencedFunction(functionName, _, _, _) =>
      functionName + "InvokeBy${title(aws_api_gateway_rest_api." + apiGateway.name + ".name)}"
  }

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "statement_id" -> TString(statementId),
    "action" -> TString("lambda:InvokeFunction"),
    "function_name" -> {
      lambda match {
        case function: ScalambdaFunction.ProjectFunction =>
          TResourceRef("aws_lambda_function", lambda.terraformLambdaResourceName, "function_name")
        case ScalambdaFunction.ReferencedFunction(functionName, _, _, _) =>
          TString(functionName)
      }
    },
    "qualifier" -> {
      lambda match {
        case function: ScalambdaFunction.ProjectFunction =>
          TNone
        case ScalambdaFunction.ReferencedFunction(functionName, qualifier, functionArn, apiGatewayConf) =>
          TString(qualifier)
      }
    },
    "principal" -> TString("apigateway.amazonaws.com"),
    "source_arn" -> TString("${aws_api_gateway_rest_api." + apiGateway.name + ".execution_arn}/*/*")
  )
}
