package io.carpe.scalambda.terraform

import java.io.PrintWriter

import io.carpe.scalambda.ScalambdaFunction

object ScalambdaTerraform {

  def writeTerraform(rootTerraformPath: String, scalambdaFunctions: List[ScalambdaFunction]): Unit = {
    // create open api for functions
    val openApi = OpenApi.forFunctions(scalambdaFunctions)

    // convert the api to yaml
    val openApiDefinition = OpenApi.apiToYaml(openApi)

    // write that yaml to the path
    val swaggerFilePath = rootTerraformPath + "/swagger.yaml"
    new PrintWriter(swaggerFilePath) { write(openApiDefinition); close() }
  }
}
