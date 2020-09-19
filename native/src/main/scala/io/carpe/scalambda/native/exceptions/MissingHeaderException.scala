package io.carpe.scalambda.native.exceptions

case class MissingHeaderException(missingHeaderName: String) extends Exception {
  override def getMessage: String = s"Request from AWS was missing the `${missingHeaderName}` header"
}
