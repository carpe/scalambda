package io.carpe.scalambda.terraform

import java.io.{File, PrintWriter}

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.terraform.ast.Definition
import io.carpe.scalambda.terraform.ast.resources.LambdaFunction

object ScalambdaTerraform {


  def writeTerraform(rootTerraformPath: String, functions: List[ScalambdaFunction]): Unit = {
    // create open api for functions
    val openApi = OpenApi.forFunctions(functions)

    // write the lambda functions to a file
    writeLambdas(functions)

    // write the swagger to a file
    writeSwagger(rootTerraformPath, openApi)
  }

  def writeLambdas(scalambdaFunctions: List[ScalambdaFunction]): Seq[Definition] = {
    scalambdaFunctions.map(function => {
      LambdaFunction(function)
    })
  }

  def writeSwagger(rootTerraformPath: String, openApi: OpenApi): Unit = {
    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }
  }
}
