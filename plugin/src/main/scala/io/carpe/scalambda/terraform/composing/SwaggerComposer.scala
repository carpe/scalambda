package io.carpe.scalambda.terraform.composing

import java.io.{File, PrintWriter}
import java.nio.file.Files

import cats.data.NonEmptyList
import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiGatewayEndpoint
import io.carpe.scalambda.terraform.OpenApi
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.TVariableRef
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.terraform.data.TemplateFile
import io.carpe.scalambda.terraform.openapi.SecurityDefinition

object SwaggerComposer {

  def writeSwagger( apiName: String,
                    rootTerraformPath: String,
                    endpointMappings: NonEmptyList[(ApiGatewayEndpoint, ScalambdaFunction)],
                    functionAliases: Seq[LambdaFunctionAlias],
                    securityDefinitions: Seq[SecurityDefinition]
                  ): TemplateFile = {

    val openApi = OpenApi.forFunctions(endpointMappings)

    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // make sure parent directories are created
    Files.createDirectories(new File(rootTerraformPath).toPath)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }

    val lambdaVars: Seq[(String, TValue)] = functionAliases.map(alias => {
      alias.swaggerVariableName -> alias.invokeArn
    })

    val securityVars: Seq[(String, TValue)] = securityDefinitions.distinct.flatMap(securityDefinition => {
      securityDefinition match {
        case authorizer: SecurityDefinition.TokenAuthorizer =>
          Seq(
            authorizer.authorizerUriVariable -> TVariableRef(authorizer.authorizerUriVariable),
            authorizer.authorizerRoleVariable -> TVariableRef(authorizer.authorizerRoleVariable)
          )
        case authorizer: SecurityDefinition.RequestAuthorizer =>
          Seq(
            authorizer.authorizerUriVariable -> TVariableRef(authorizer.authorizerUriVariable),
            authorizer.authorizerRoleVariable -> TVariableRef(authorizer.authorizerRoleVariable)
          )
        case SecurityDefinition.ApiKey =>
          Nil
      }
    })

    val allVars = lambdaVars ++ securityVars

    TemplateFile("swagger.yaml", apiName, allVars)
  }
}
