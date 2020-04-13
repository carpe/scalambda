package io.carpe.scalambda.terraform.composing

import java.io.File

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TString, TVariableRef}
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway._
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaPermission
import io.carpe.scalambda.terraform.ast.providers.aws.route53.Route53Record
import io.carpe.scalambda.terraform.ast.providers.terraform.data.TemplateFile

object ApiGatewayComposer {

  def maybeDefineApiResources(
                               isXrayEnabled: Boolean,
                               apiName: String,
                               functions: Seq[ScalambdaFunction],
                               functionAliases: Seq[LambdaFunctionAlias],
                               terraformOutput: File,
                               maybeDomainName: Option[String]
                             ): (
    Option[ApiGateway],
      Option[TemplateFile],
      Seq[LambdaPermission],
      Option[ApiGatewayDeployment],
      Option[ApiGatewayStage],
      Option[ApiGatewayDomainName],
      Option[ApiGatewayBasePathMapping],
      Option[Route53Record],
      Seq[Variable[_]]
    ) = {

    // if there are no functions that are configured to be exposed via api
    if (functions.count(_.apiGatewayConfig.isDefined) == 0) {
      // do an early return
      return (None, None, Seq.empty, None, None, None, None, None, Seq.empty)
    }

    val certificateArnVariable = Variable("certificate_arn", Some("Arn of AWS Certificate Manager certificate."), None)

    val domainNameToggleVariable = Variable[TBool](
      name = "enable_domain_name",
      description = Some("If set to true, this will link the latest api deployment to the domain name. You will likely only want this value set to true while in the production environment."),
      defaultValue = Some(TBool(false))
    )

    val zoneIdVariable = Variable[TString](
      name = "zone_id",
      description = Some("This zone id is required in order to create the custom domain name mapping that allows for you ApiGateway deployment to have a pretty url."),
      defaultValue = None
    )

    val swaggerTemplate = {
      SwaggerComposer.writeSwagger(apiName = apiName, rootTerraformPath = terraformOutput.getAbsolutePath, functions = functions, functionAliases = functionAliases)
    }

    // define api gateway
    val api = ApiGateway(TString(apiName), None, swaggerTemplate)

    // define permissions for api gateway to invoke lambdas
    val permissions = functionAliases.map(alias => {
      val statementId = s"Allow${alias.approximateFunctionName}InvokeByApi" + "${title(terraform.workspace)}"
      val resourceName = alias.name
      val apiArn = TString("${aws_api_gateway_rest_api." + api.name + ".execution_arn}/*/*")
      val principal = "apigateway.amazonaws.com"
      LambdaPermission(resourceName, statementId, principal, alias.functionName, alias.qualifier, apiArn)
    })

    // create deployment
    val apiGatewayDeployment = ApiGatewayDeployment(api, permissions)

    // create stage
    val apiGatewayStage = ApiGatewayStage(api, apiGatewayDeployment, isXrayEnabled)

    // create resources for domain mapping (if domain name was supplied)
    val maybeApiGatewayDomainName = maybeDomainName.map(domainName => ApiGatewayDomainName("api_domain", domainName, TVariableRef(certificateArnVariable.name), domainNameToggleVariable))
    val maybeReferrableApiGatewayDomainName = maybeApiGatewayDomainName.map(domainName => domainName.copy(name = s"${domainName.name}[count.index]"))
    val maybeBasePathMapping = maybeReferrableApiGatewayDomainName.map(domainName => {
      apigateway.ApiGatewayBasePathMapping(api, apiGatewayDeployment, domainName, domainNameToggleVariable)
    })

    val zoneId = TVariableRef(zoneIdVariable.name)
    val maybeRoute53Record = maybeReferrableApiGatewayDomainName.map(domainName => Route53Record("api_domain_alias", domainName, zoneId, domainNameToggleVariable))

    val certificate = maybeDomainName
      .map(_ => certificateArnVariable)
      .toSeq
    val domainNameToggle = maybeDomainName
      .map(_ => domainNameToggleVariable)
      .toSeq
    val hostedZoneId = maybeDomainName
      .map(_ => zoneIdVariable)
      .toSeq

    (
      Some(api),
      Some(swaggerTemplate),
      permissions,
      Some(apiGatewayDeployment),
      Some(apiGatewayStage),
      maybeApiGatewayDomainName,
      maybeBasePathMapping,
      maybeRoute53Record,
      certificate ++ domainNameToggle ++ hostedZoneId
    )
  }
}
