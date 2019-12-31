package io.carpe.scalambda.request

import io.circe.CursorOp.DownField

case class APIGatewayProxyRequest[T](resource: String,
                                     path: String,
                                     httpMethod: String,
                                     headers: Map[String, String],
                                     queryStringParameters: Map[String, String],
                                     pathParameters: Map[String, String],
                                     stageVariables: Map[String, String],
                                     requestContext: RequestContext,
                                     body: Option[T],
                                     isBase64Encoded: Option[Boolean]
                                    )

object APIGatewayProxyRequest {

  import io.circe._
  import io.circe.parser._

  implicit def decode[T](implicit typeDecoder: Decoder[T]): Decoder[APIGatewayProxyRequest[T]] = {

    Decoder.instance[APIGatewayProxyRequest[T]] { c: HCursor =>
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
        APIGatewayProxyRequest[T](
          resource, path, httpMethod, headers, queryStringParameters, pathParameters, stageVariables,
          requestContext, body, isBase64Encoded
        )
      }
    }
  }
}
