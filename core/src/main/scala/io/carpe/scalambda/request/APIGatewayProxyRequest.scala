package io.carpe.scalambda.request

import io.circe.CursorOp.DownField

sealed trait APIGatewayProxyRequest[+T] {
  def resource: String
  def path: String
  def httpMethod: String
  def headers: Map[String, String]
  def queryStringParameters: Map[String, String]
  def pathParameters: Map[String, String]
  def stageVariables: Map[String, String]
  def requestContext: RequestContext
  def body: Option[T]
  def isBase64Encoded: Option[Boolean]
}

object APIGatewayProxyRequest {

  case class WithBody[T](resource: String,
    path: String,
    httpMethod: String,
    headers: Map[String, String],
    queryStringParameters: Map[String, String],
    pathParameters: Map[String, String],
    stageVariables: Map[String, String],
    requestContext: RequestContext,
    body: Option[T],
    isBase64Encoded: Option[Boolean]
  ) extends APIGatewayProxyRequest[T]

  case class WithoutBody(resource: String,
                         path: String,
                         httpMethod: String,
                         headers: Map[String, String],
                         queryStringParameters: Map[String, String],
                         pathParameters: Map[String, String],
                         stageVariables: Map[String, String],
                         requestContext: RequestContext,
                         isBase64Encoded: Option[Boolean]
                        ) extends APIGatewayProxyRequest[Nothing] {
    override def body: Option[Nothing] = None
  }

  import io.circe._
  import io.circe.parser._

  implicit def decodeWithBody[T](implicit typeDecoder: Decoder[T]): Decoder[APIGatewayProxyRequest.WithBody[T]] = {

    Decoder.instance[APIGatewayProxyRequest.WithBody[T]] { c: HCursor =>
      for {
        resource <- c.downField("resource").as[String]
        path <- c.downField("path").as[String]
        httpMethod <- c.downField("httpMethod").as[String]
        headers <- c.downField("headers").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        queryStringParameters <- c.downField("queryStringParameters").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        pathParameters <- c.downField("pathParameters").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        stageVariables <- c.downField("stageVariables").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        requestContext <- c.downField("requestContext").as[RequestContext]
        body <- c.downField("body").as[Option[String]].flatMap {
          case Some(stringlyJson) =>
            // API Proxy Requests send the Json body for all requests as a string. So we must attempt to decode the
            // body that was supplied
            parse(stringlyJson).fold(
              f => Left(DecodingFailure(
                s"The request body must be a stringified JSON object. Parsing failed: ${f.message}", List(DownField("body"))
              )),
              s => s.as[T](typeDecoder).map(Some(_))
            )
          case None =>
            // No body was supplied, so there is no need to attempt to decode it
            Right(None)
        }
        isBase64Encoded <- c.downField("isBase64Encoded").as[Option[Boolean]]
      } yield {
        APIGatewayProxyRequest.WithBody[T](
          resource, path, httpMethod, headers, queryStringParameters, pathParameters, stageVariables,
          requestContext, body, isBase64Encoded
        )
      }
    }
  }

  implicit def decodeWithoutBody: Decoder[APIGatewayProxyRequest.WithoutBody] = {
    Decoder.instance[APIGatewayProxyRequest.WithoutBody] { c: HCursor =>
      for {
        resource <- c.downField("resource").as[String]
        path <- c.downField("path").as[String]
        httpMethod <- c.downField("httpMethod").as[String]
        headers <- c.downField("headers").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        queryStringParameters <- c.downField("queryStringParameters").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        pathParameters <- c.downField("pathParameters").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        stageVariables <- c.downField("stageVariables").as[Option[Map[String, String]]].map(_.getOrElse(Map()))
        requestContext <- c.downField("requestContext").as[RequestContext]
        isBase64Encoded <- c.downField("isBase64Encoded").as[Option[Boolean]]
      } yield {
        APIGatewayProxyRequest.WithoutBody(
          resource, path, httpMethod, headers, queryStringParameters, pathParameters, stageVariables,
          requestContext, isBase64Encoded
        )
      }
    }
  }
}
