package io.carpe.scalambda.terraform.composing

import java.io.PrintWriter

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.OpenApi
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.terraform.data.TemplateFile

object SwaggerComposer {

  def writeSwagger( apiName: String,
                    rootTerraformPath: String,
                    functions: Seq[ScalambdaFunction],
                    functionAliases: Seq[LambdaFunctionAlias]
                  ): TemplateFile = {
    val openApi = OpenApi.forFunctions(functions)

    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }

    val lambdaVars: Seq[(String, TValue)] = functionAliases.map(alias => {
      alias.swaggerVariableName -> alias.invokeArn
    })

    TemplateFile("swagger.yaml", apiName, lambdaVars)
  }
}
