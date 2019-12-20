package io.carpe.scalambda.request

import io.circe.{Decoder, Encoder}

case class RequestContextIdentity( cognitoIdentityPoolId: Option[String],
                                   accountId: Option[String],
                                   cognitoIdentityId: Option[String],
                                   caller: Option[String],
                                   apiKey: Option[String],
                                   sourceIp: String,
                                   cognitoAuthenticationType: Option[String],
                                   cognitoAuthenticationProvider: Option[String],
                                   userArn: Option[String],
                                   userAgent: Option[String],
                                   user: Option[String]
                                 )

object RequestContextIdentity {
  import io.circe.generic.semiauto._

  implicit val encode: Encoder[RequestContextIdentity] = deriveEncoder[RequestContextIdentity]
  implicit val decode: Decoder[RequestContextIdentity] = deriveDecoder[RequestContextIdentity]
}

