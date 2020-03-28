package io.carpe.scalambda.api.validation

import io.carpe.scalambda.response.ApiError
import io.circe.Json


trait ValidationError {

  /**
   * Pointer to the specific field that was invalid for the given record
   * @return
   */
  def pointer: Option[String]

  /**
   * Message to show to users to help them determine how to solve the issue
   * @return
   */
  def message: String


  def toApiError: ApiError = {
    val validationMessage = message
    new ApiError {
      override val httpStatus: Int = 422
      override val message: String = validationMessage
      override val additional: Option[Json] = pointer.map(p =>
        Json.obj("pointer" -> Json.fromString(p))
      )
    }
  }
}
