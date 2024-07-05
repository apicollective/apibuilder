package helpers

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec

trait ValidatedTestHelpers {

  def expectValid[T, U](r: ValidatedNec[T, U]): U = {
    r match {
      case Valid(o) => o
      case Invalid(errors) => sys.error(s"Expected valid but was invalid: ${errors.toNonEmptyList}")
    }
  }

  def expectInvalid[T, U](r: ValidatedNec[T, U]): Seq[T] = {
    r match {
      case Valid(_) => sys.error("Expected invalid but was valid")
      case Invalid(errors) => errors.toNonEmptyList.toList
    }
  }

}
