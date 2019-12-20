package io.carpe.scalambda.request

trait APIGatewayProxyRequestBase[T] {
  def resource: String
  def path: String
  def httpMethod: String
  def headers: Map[String, String]
  def queryStringParameters: Option[Map[String, String]]
  def pathParameters: Option[Map[String, String]]
  def stageVariables: Option[Map[String, String]]
  def body: T
  def isBase64Encoded: Option[Boolean]
}
