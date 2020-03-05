package io.carpe.scalambda.terraform

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.function.{ApiGatewayConf, FunctionConf, Method}
import org.scalatest.flatspec.AnyFlatSpec

class OpenApiSpec extends AnyFlatSpec {

  "OpenApi" should "be able to be serialized as yaml" in {
    val functions = List(
      ScalambdaFunction(
        "CarsIndex", "io.cars.index.CarsIndex",
        functionConfig = FunctionConf.carpeDefault,
        apiConfig = Some(ApiGatewayConf(route = "/cars", method = Method.GET)),
        s3BucketName = "testing"
      )
    )

    val testApi = OpenApi.forFunctions(functions)

    val expectedOutput =
      """swagger: '2.0'
        |info:
        |  version: latest
        |  title: ${api_name}
        |schemes:
        |- https
        |paths:
        |  /cars:
        |    get:
        |      tags: []
        |      description: TBD
        |      consumes:
        |      - application/json
        |      security:
        |      - carpeAuthorizer: []
        |      responses:
        |        '200':
        |          description: Request completed without errors!
        |      x-amazon-apigateway-integration:
        |        uri: ${cars_index_lambda_invoke_arn}
        |        passthroughBehavior: when_no_match
        |        httpMethod: POST
        |        type: aws_proxy
        |    options:
        |      tags:
        |      - Meta
        |      description: Used to handle CORS on UI
        |      consumes:
        |      - application/json
        |      security:
        |      - carpeAuthorizer: []
        |      responses:
        |        '200':
        |          description: Default response for CORS method
        |          headers:
        |            Access-Control-Allow-Headers:
        |              type: string
        |            Access-Control-Allow-Methods:
        |              type: string
        |            Access-Control-Allow-Origin:
        |              type: string
        |            Access-Control-Max-Age:
        |              type: integer
        |      x-amazon-apigateway-integration:
        |        type: mock
        |        requestTemplates:
        |          application/json: '{"statusCode":200}'
        |        responses:
        |          default:
        |            statusCode: '200'
        |            responseParameters:
        |              method.response.header.Access-Control-Allow-Headers: '''Content-Type,X-Amz-Date,Authorization,X-Api-Key'''
        |              method.response.header.Access-Control-Allow-Methods: '''*'''
        |              method.response.header.Access-Control-Allow-Origin: '''*'''
        |              method.response.header.Access-Control-Max-Age: '''600'''
        |            responseTemplates:
        |              application/json: '{}'
        |""".stripMargin

    assert(OpenApi.apiToYaml(testApi) === expectedOutput)
  }


}