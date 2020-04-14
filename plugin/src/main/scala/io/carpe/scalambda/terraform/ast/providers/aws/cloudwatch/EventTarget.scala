package io.carpe.scalambda.terraform.ast.providers.aws.cloudwatch

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{THeredoc, TResourceRef, TString}
import io.circe.Json

case class EventTarget(namePrefix: String, rule: EventRule, functionName: TValue, qualifiedArn: TValue, json: Json) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override def resourceType: String = "aws_cloudwatch_event_target"

  /**
   * Examples: "my_lambda_function" "my_iam_role"
   *
   * @return
   */
  override def name: String = StringUtils.toSnakeCase(namePrefix)

  /**
   * Properties of the definition
   */
  override def body: Map[String, TValue] = Map(
    "rule" -> TResourceRef(rule, "name"),
    "target_id" -> functionName,
    "arn" -> qualifiedArn,
    "input" -> THeredoc(json.noSpaces)
  )
}
