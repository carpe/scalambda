package io.carpe.scalambda.terraform

import org.scalatest.flatspec.AnyFlatSpec

class OpenApiSpec extends AnyFlatSpec {

  "OpenApi" should "be able to be serialized as yaml" in {
    val functions = List()

    val testApi = OpenApi.forFunctions(functions)

    val expectedOutput =
      """swagger: '2.0'
        |info:
        |  version: latest
        |  title: ${api_name}
        |schemes:
        |- https
        |""".stripMargin

    assert(OpenApi.apiToYaml(testApi) === expectedOutput)
  }

}
