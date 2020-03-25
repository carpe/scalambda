package io.carpe.scalambda.response

import io.circe.{Encoder, Json}

/**
 * This is an optional Error class you can use as your Error return type if you wish. You can then extend it with your
 * own error types that implement / override the members. This makes it simple to return error types from your handler
 */
abstract class ApiError extends Throwable {

  /**
   * The HTTP status code of the error
   */
  val httpStatus: Int = 400

  /**
   * The headers you want to go with the error.
   */
  val headers: Map[String, String] = Map.empty

  /**
   * Use errorCode for when you might have many 400 HTTP error types and you need your callers to be able to
   * reliably convert this 400 Error into some specific error type on their system so they can handle the response
   * correctly to the specific error.
   */
  val errorCode: Option[Int] = None

  /**
   * The json in this field will be merged with the standard error fields.
   */
  val additional: Option[Json] = None

  /**
   * The plain text string of what happened. Great for humans, bad for systems - use errorCodes when a system needs
   * to respond to the error in a specific way.
   */
  val message: String
}

object ApiError {

  val defaultEncoder: Encoder[ApiError] = Encoder[ApiError](a => {
    val requiredJson = Json.obj(
      ("status", Json.fromInt(a.httpStatus)),
      ("message", Json.fromString(a.message))
    )

    a.additional.fold(requiredJson)(d => requiredJson.deepMerge(d))
  })

  /**
   * An Error that is uninformative by design. Use this error when the true cause of the error must be hidden from users
   * due to security reasons.
   *
   * Note: Please don't overuse this error! Meaningful error messages can be helpful for users.
   */
  case object InternalError extends ApiError {
    override val httpStatus: Int = 500
    override val message: String = "Internal Server Error"
  }

  /**
   * A super basic InputError.
   *
   * @param msg explanation for the user
   */
  case class InputError(msg: String) extends ApiError {
    override val httpStatus: Int = 422
    override val message: String = msg
  }

  /**
   * A basic NotFound error.
   *
   * @param msg explanation for the user
   */
  case class NotFoundError(msg: String) extends ApiError {
    override val httpStatus: Int = 404
    override val message: String = msg
  }
}

