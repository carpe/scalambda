package io.carpe.scalambda.api.validation

import cats.Semigroup
import cats.data.{Validated, ValidatedNec}
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.api.conf.ScalambdaApi
import io.carpe.scalambda.api.validation.Validation.ValidationResult

trait Validation[C <: ScalambdaApi, A] extends LazyLogging {

  /**
   * Run validations against the record.
   * @param record to validate
   * @return
   */
  def run(record: A)(implicit api: C): ValidationResult[A]

}

object Validation {
  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  implicit def validationSemigroup[C <: ScalambdaApi, A]: Semigroup[Validation[C, A]] = new Semigroup[Validation[C, A]] {
    implicit val takeLatest: Semigroup[A] = (_: A, y: A) => y

    import cats.implicits._

    override def combine(x: Validation[C, A], y: Validation[C, A]): Validation[C, A] = {
      new Validation[C, A] {
        override def run(record: A)(implicit api: C): ValidationResult[A] = {

          // perform the first validation
          val firstValidation: ValidatedNec[ValidationError, A] = x.run(record)(api)

          // perform the second validation with either the original input, or with the result of the previous validation
          val inputToSecondValidation = firstValidation.fold(_ => record, identity)
          val secondValidation: ValidatedNec[ValidationError, A] = y.run(inputToSecondValidation)(api)

          // combine the results
          firstValidation |+| secondValidation
        }
      }
    }
  }
}
