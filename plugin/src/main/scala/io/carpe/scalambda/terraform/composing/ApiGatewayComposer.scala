package io.carpe.scalambda.terraform.composing

import java.io.File

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.props.TValue.TString
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.LambdaPermission
import io.carpe.scalambda.terraform.ast.providers.terraform.data.TemplateFile
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway
import io.carpe.scalambda.terraform.ast.providers.aws.apigateway._

object ApiGatewayComposer {

  def maybeDefineApiResources(
                               isXrayEnabled: Boolean,
                               apiName: String,
                               authorizerArn: String,
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
      Seq[Variable[_]]
    ) = {

    // if there are no functions that are configured to be exposed via api
    if (functions.count(_.apiGatewayConfig.isDefined) == 0) {
      // do an early return
      return (None, None, Seq.empty, None, None, None, None, Seq.empty)
    }

    val swaggerTemplate = {
      SwaggerComposer.writeSwagger(apiName = apiName, rootTerraformPath = terraformOutput.getAbsolutePath, functions = functions, functionAliases = functionAliases, authorizerArn = authorizerArn)
    }

    // define api gateway
    val api = ApiGateway(TString(apiName), None, swaggerTemplate)

    // define permissions for api gateway to invoke lambdas
    val permissions = functionAliases.map(alias => {
      val statementId = "Allow${title(" + alias.functionName.toString + ")}InvokeByApi"
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
    val maybeApiGatewayDomainName = maybeDomainName.map(domainName => ApiGatewayDomainName(domainName))
    val maybeBasePathMapping = maybeApiGatewayDomainName.map(domainName => {
      apigateway.ApiGatewayBasePathMapping(api, apiGatewayDeployment, domainName)
    })
    val certificateVariable = maybeDomainName
      .map(_ => Variable("certificate_arn", Some("Arn of AWS Certificate Manager certificate."), None))
      .toSeq

    (
      Some(api),
      Some(swaggerTemplate),
      permissions,
      Some(apiGatewayDeployment),
      Some(apiGatewayStage),
      maybeApiGatewayDomainName,
      maybeBasePathMapping,
      certificateVariable
    )
  }
}
