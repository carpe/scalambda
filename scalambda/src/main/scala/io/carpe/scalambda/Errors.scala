package io.carpe.scalambda

import io.circe.{Encoder, Json}

/**
 * This is an optional Error class you can use as your Error return type if you wish. You can then extend it with your
 * own error types that implement / override the members. This makes it simple to return error types from your handler
 */
abstract class Errors {

  /**
   * The HTTP status code of the error
   */
  val httpStatus: Int = 400

  /**
   * The headers you want to go with the error.
   */
  val headers: Option[Map[String, String]] = None

  /**
   * Use errorCode for when you might have many 400 HTTP error types and you need your callers to be able to
   * reliably convert this 400 Error into some specific error type on their system so they can handle the response
   * correctly to the specific error.
   */
  val errorCode: Option[Int] = None

  /**
   * Use the optional data field when you need to pass back some specific Json value of additional data in the
   * error that the caller might need when processing what to do with the error.
   */
  val data: Option[Json] = None

  /**
   * The plain text string of what happened. Great for humans, bad for systems - use errorCodes when a system needs
   * to respond to the error in a specific way.
   */
  val message: String

  def toResponse = APIGatewayProxyResponse(httpStatus, headers, Some(this))
}

object Errors {
  implicit val encoder: Encoder[Errors] = Encoder[Errors](a => {
    val requiredJson = Json.obj(("errorCode", a.errorCode.map(x => Json.fromInt(x)).getOrElse(Json.Null)),
      ("message", Json.fromString(a.message)))
    a.data.fold(requiredJson)(d => requiredJson.deepMerge(Json.obj("data" -> d)))
  })

  /**
   * A super basic InputError type. This is used with the TypedError proxies for when a basic HTTP 400 with no error code
   * is sufficient for the error.
   *
   * @param msg
   */
  case class InputError(msg: String) extends Errors {
    override val message: String = msg
  }
}

