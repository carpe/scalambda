package io.carpe.scalambda.terraform.composing

import io.carpe.scalambda.terraform.ast.Definition
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TResourceRef}
import io.carpe.scalambda.terraform.ast.providers.aws.cloudwatch.EventRule.Schedule
import io.carpe.scalambda.terraform.ast.providers.aws.cloudwatch.{EventRule, EventTarget}
import io.carpe.scalambda.terraform.ast.providers.aws.iam.Role
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaPermission
import io.circe.Json

object WarmerComposer {

  case class Warmer(
    warmerRole: Role,
    eventRule: EventRule,
    eventTarget: EventTarget,
    lambdaPermission: LambdaPermission
  ) {
    def definitions: Seq[Definition] = Seq(
      warmerRole, eventRule, eventTarget, lambdaPermission
    )
  }

  def composeWarmer(lambdaAlias: LambdaFunctionAlias, enableWarmers: Variable[TBool], json: Json): Warmer = {

    val role = Role(
      namePrefix = s"${lambdaAlias.approximateFunctionName}WarmerRole",
      policy = """{
                 |  "Version": "2012-10-17",
                 |  "Statement": [
                 |    {
                 |      "Sid": "",
                 |      "Effect": "Allow",
                 |      "Principal": {
                 |        "Service": "events.amazonaws.com"
                 |      },
                 |      "Action": "sts:AssumeRole"
                 |    }
                 |  ]
                 |}""".stripMargin,
      description =
        s"Role assumed by Cloudwatch Event for keeping the ${lambdaAlias.approximateFunctionName} lambda warm"
    )

    val eventRule = EventRule(
      namePrefix = s"${lambdaAlias.approximateFunctionName}Warmer",
      description = s"Warmer for the ${lambdaAlias.approximateFunctionName} function",
      role = TResourceRef(role, "arn"),
      isEnabled = enableWarmers.ref,
      schedule = Schedule.Rate(minutes = 3)
    )

    val eventTarget = EventTarget(
      namePrefix = lambdaAlias.approximateFunctionName,
      rule = eventRule,
      functionName = lambdaAlias.functionName,
      qualifiedArn = lambdaAlias.qualifiedArn,
      json
    )

    val lambdaPermission = LambdaPermission(
      lambdaResourceName = s"${lambdaAlias.name}_warmer",
      statementId = "Allow${title(" + lambdaAlias.functionName.asInterpolatedRef + ")}InvokeByWarmer",
      principal = "events.amazonaws.com",
      functionName = lambdaAlias.functionName,
      qualifer = lambdaAlias.qualifier,
      sourceArn = TResourceRef(eventRule, "arn")
    )

    Warmer(role, eventRule, eventTarget, lambdaPermission)
  }
}
