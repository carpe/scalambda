package io.carpe.scalambda.terraform

import io.carpe.scalambda.fixtures.ScalambdaFunctionFixtures
import org.scalatest.flatspec.AnyFlatSpec

class OpenApiSpec extends AnyFlatSpec with ScalambdaFunctionFixtures {

  "OpenApi" should "be able to be serialized as yaml" in {
    val functions = List(
      carsIndexFunction
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
        |      - my_authorizer: []
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
        |      security: []
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
        |securityDefinitions:
        |  my_authorizer:
        |    type: apiKey
        |    name: Authorization
        |    in: header
        |    x-amazon-apigateway-authtype: custom
        |    x-amazon-apigateway-authorizer:
        |      authorizerUri: arn:fake:my_authorizer/invocations
        |      authorizerCredentials: arn:fake:MyRole
        |      authorizerResultTtlInSeconds: 300
        |      identityValidationExpression: ^Bearer [-0-9a-zA-z\.]*$
        |      type: token
        |""".stripMargin

    val actual = OpenApi.apiToYaml(testApi)

    assert(actual === expectedOutput)
  }


}