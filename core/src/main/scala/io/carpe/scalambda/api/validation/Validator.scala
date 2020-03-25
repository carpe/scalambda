package io.carpe.scalambda.api.validation

import cats.data.{Chain, Validated}
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.response.ApiErrors

case class Validator[C <: ScalambdaApi, A](validations: Chain[Validation[C, A]]) {

  import cats.data.NonEmptyChain

  lazy val maybeComposedValidations: Option[Validation[C, A]] = {
    NonEmptyChain.fromChain(validations).map(_.reduce)
  }

  def validate(record: A)(implicit api: C): Either[ApiErrors, A] = {
    maybeComposedValidations.map(composedValidations => {
      val validationResults = composedValidations.run(record)

      validationResults match {
        case Validated.Valid(validatedRecord) =>
          // validations all succeeded, so we return the potentially mapped record
          Right(validatedRecord)
        case Validated.Invalid(errors) =>
          // one or more validations failed, so we map all of the validations into a list of errors to send to the user
          Left(new ApiErrors(errors.map(_.toApiError)))
      }
    }).getOrElse(Right(record))
  }

}
