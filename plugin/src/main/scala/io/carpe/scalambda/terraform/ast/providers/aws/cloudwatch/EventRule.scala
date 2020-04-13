package io.carpe.scalambda.terraform.ast.providers.aws.cloudwatch

import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition.Resource
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TString}
import io.carpe.scalambda.terraform.ast.providers.aws.cloudwatch.EventRule.Schedule

case class EventRule(namePrefix: String, description: String, role: TValue, isEnabled: TValue, schedule: Schedule) extends Resource {
  /**
   * Examples: "aws_lambda_function" "aws_iam_role"
   *
   * Can be null in the case of terraform modules!
   */
  override def resourceType: String = "aws_cloudwatch_event_rule"

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
    "name" -> TString(namePrefix + "${title(terraform.workspace)}"),
    "description" -> TString(description),
    "schedule_expression" -> schedule.asExpression,
    "role_arn" -> role,
    "is_enabled" -> isEnabled
  )

}

object EventRule {
  sealed trait Schedule {
    def asExpression: TValue
  }

  object Schedule {
    case class Rate(minutes: Int) extends Schedule {
      override def asExpression: TValue = TString(s"rate(${minutes} minutes)")
    }
  }
}