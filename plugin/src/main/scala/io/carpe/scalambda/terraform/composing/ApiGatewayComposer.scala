package io.carpe.scalambda.terraform.composing

import java.io.File

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiDomain
import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TBool, TResourceRef, TString, TVariableRef}
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway._
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.{LambdaFunctionAliasResource, LambdaPermission}
import io.carpe.scalambda.terraform.ast.providers.aws.route53.Route53Record
import io.carpe.scalambda.terraform.ast.providers.terraform.data.TemplateFile

object ApiGatewayComposer {

  /**
   * This will be the name of the alias that api gateway uses to invoke the non-referenced lambda functions
   */
  lazy val apiFunctionAlias: String = "api"

  def maybeDefineApiResources(
                               isXrayEnabled: Boolean,
                               apiName: String,
                               functions: Seq[ScalambdaFunction],
                               functionAliases: Seq[LambdaFunctionAlias],
                               terraformOutput: File,
                               apiDomainMapping: ApiDomain
  ): (
    Option[ApiGateway],
    Option[TemplateFile],
    Seq[LambdaFunctionAlias],
    Seq[LambdaPermission],
    Option[ApiGatewayDeployment],
    Option[ApiGatewayStage],
    Option[ApiGatewayDomainName],
    Option[ApiGatewayBasePathMapping],
    Option[Route53Record],
    Seq[Variable[_]],
    Seq[Output[_]]
  ) = {

    // if there are no functions that are configured to be exposed via api
    if (functions.count(_.apiGatewayConfig.isDefined) == 0) {
      // do an early return
      return (None, None, Nil, Nil, None, None, None, None, None, Nil, Nil)
    }

    val domainNameVariable = Variable(ApiDomain.FromVariable.apiDomainVariableName, Some("Top-level domain name to map the service to."), None)

    val certificateArnVariable = Variable("certificate_arn", Some("Arn of AWS Certificate Manager certificate."), None)

    val domainNameToggleVariable = Variable[TBool](
      name = "enable_domain_name",
      description = Some(
        "If set to true, this will link the latest api deployment to the domain name. You will likely only want this value set to true while in the production environment."
      ),
      defaultValue = Some(TBool(false))
    )

    val zoneIdVariable = Variable[TString](
      name = "zone_id",
      description = Some(
        "This zone id is required in order to create the custom domain name mapping that allows for you ApiGateway deployment to have a pretty url."
      ),
      defaultValue = None
    )

    // defined aliases for all functions in this project, so that as the versions of those functions change
    // we can make sure that the api is never pointed at a non-defined alias
    val apiFunctionAliases = functionAliases.flatMap(functionAlias => {
      functionAlias match {
        case projectFunctionAlias: LambdaFunctionAliasResource =>
          Some(
            projectFunctionAlias.copy(
              aliasName = apiFunctionAlias,
              description = s"This is the version of the lambda used by ApiGateway for $apiName."
            )
          )
        case _ =>
          None
      }
    })

    // create aliases for the inverse of the above aliases
    val referencedFunctionAliases = functionAliases.flatMap(functionAlias => {
      functionAlias match {
        case projectFunctionAlias: LambdaFunctionAliasResource =>
          None
        case referenced: LambdaFunctionAlias =>
          Some(referenced)
      }
    })

    // this will be the full list of aliases used by the api to invoke the lambdas
    val apiAliases = apiFunctionAliases ++ referencedFunctionAliases

    // use the aliases to define the swagger template that will be used to generate our api
    val swaggerTemplate = {
      SwaggerComposer.writeSwagger(
        apiName = apiName,
        rootTerraformPath = terraformOutput.getAbsolutePath,
        functions = functions,
        functionAliases = apiAliases
      )
    }

    // define api gateway
    val api = ApiGateway(TString(apiName), None, swaggerTemplate)

    // define permissions for both of the defined aliases for api gateway to use to invoke lambdas
    val permissions = apiAliases.map(alias => {
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
    val maybeApiGatewayDomainName = apiDomainMapping.map(
      domainName =>
        ApiGatewayDomainName(
          "api_domain",
          domainName,
          TVariableRef(certificateArnVariable.name),
          domainNameToggleVariable
        )
    )

    val maybeReferrableApiGatewayDomainName =
      maybeApiGatewayDomainName.map(domainName => domainName.copy(name = s"${domainName.name}[count.index]"))
    val maybeBasePathMapping = maybeReferrableApiGatewayDomainName.map(domainName => {
      apigateway.ApiGatewayBasePathMapping(api, apiGatewayStage, domainName, domainNameToggleVariable)
    })

    val zoneId = TVariableRef(zoneIdVariable.name)
    val maybeRoute53Record = maybeReferrableApiGatewayDomainName.map(
      domainName => Route53Record("api_domain_alias", domainName, zoneId, domainNameToggleVariable)
    )


    val domainName = apiDomainMapping match {
      case ApiDomain.FromVariable =>
        Seq(domainNameVariable)
      case ApiDomain.Unmapped =>
        Nil
      case ApiDomain.Static(domain) =>
        Nil
    }
    val certificate = apiDomainMapping
      .map(_ => certificateArnVariable)
      .toSeq
    val domainNameToggle = apiDomainMapping
      .map(_ => domainNameToggleVariable)
      .toSeq
    val hostedZoneId = apiDomainMapping
      .map(_ => zoneIdVariable)
      .toSeq

    (
      Some(api),
      Some(swaggerTemplate),
      apiFunctionAliases,
      permissions,
      Some(apiGatewayDeployment),
      Some(apiGatewayStage),
      maybeApiGatewayDomainName,
      maybeBasePathMapping,
      maybeRoute53Record,
      domainName ++ certificate ++ domainNameToggle ++ hostedZoneId,
      composeOutputs(api, apiGatewayDeployment, apiGatewayStage)
    )
  }

  private def composeOutputs(api: ApiGateway, deployment: ApiGatewayDeployment, apiGatewayStage: ApiGatewayStage): Seq[Output[_]] = {
    Seq(
      Output("rest_api_id", Some("id of the created api gateway rest api"), isSensitive = false, TResourceRef(api, "id")),
      Output("rest_api_deployment_id", Some("id of the created api gateway rest api deployment"), isSensitive = false, TResourceRef(deployment, "id")),
      Output("rest_api_stage_name", Some("name of the created api gateway rest api stage"), isSensitive = false, TResourceRef(apiGatewayStage, "stage_name"))
    )
  }
}
