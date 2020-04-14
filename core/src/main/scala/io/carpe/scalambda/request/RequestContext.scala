package io.carpe.scalambda.request

import io.circe.{Decoder, Encoder}

sealed trait RequestContext {
  def path: Option[String]
  def accountId: Option[Long]
  def resourceId: Option[String]
  def stage: String
  def requestId: Option[String]
  def identity: RequestContextIdentity
  def resourcePath: String
  def httpMethod: String
  def apiId: Option[String]
}

object RequestContext {

  case class Authenticated(path: Option[String], accountId: Option[Long], resourceId: Option[String], stage: String,
                           requestId: Option[String],  identity: RequestContextIdentity, resourcePath: String,
                           httpMethod: String, apiId: Option[String],
                           authorizer: Map[String, Either[Int, String]]
                          ) extends RequestContext

  case class Unauthenticated(path: Option[String], accountId: Option[Long], resourceId: Option[String],
                             stage: String, requestId: Option[String], identity: RequestContextIdentity,
                             resourcePath: String, httpMethod: String, apiId: Option[String]
                            ) extends RequestContext



  import io.circe.generic.semiauto._
  import io.circe.syntax._

  implicit val encodeIntOrString: Encoder[Either[Int, String]] =
    Encoder.instance(_.fold(_.asJson, _.asJson))

  implicit val decodeIntOrString: Decoder[Either[Int, String]] =
    Decoder[Int].map(Left(_)).or(Decoder[String].map(Right(_)))

  implicit val encodeAuthenticated: Encoder[Authenticated] = deriveEncoder[Authenticated]
  implicit val decodeAuthenticated: Decoder[Authenticated] = deriveDecoder[Authenticated]

  implicit val encodeAnon: Encoder[Unauthenticated] = deriveEncoder[Unauthenticated]
  implicit val decodeAnon: Decoder[Unauthenticated] = deriveDecoder[Unauthenticated]

  implicit val decode: Decoder[RequestContext] = {
    case class MaybeAuthenticated(path: Option[String], accountId: Option[Long], resourceId: Option[String], stage: String,
                                  requestId: Option[String],  identity: RequestContextIdentity, resourcePath: String,
                                  httpMethod: String, apiId: Option[String],
                                  authorizer: Option[Map[String, Either[Int, String]]]
                                 ) {
      def asContext: RequestContext = {
        if (authorizer.exists(_.nonEmpty)) {
          return Authenticated(path, accountId, resourceId, stage, requestId, identity, resourcePath, httpMethod, apiId, authorizer.getOrElse(Map.empty))
        }
        Unauthenticated(path, accountId, resourceId, stage, requestId, identity, resourcePath, httpMethod, apiId)
      }
    }

    deriveDecoder[MaybeAuthenticated].map(_.asContext)
  }

}