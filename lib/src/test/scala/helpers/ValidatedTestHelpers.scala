package helpers

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec

trait ValidatedTestHelpers {

  def expectValid[T](r: ValidatedNec[_, T]): T = {
    r match {
      case Valid(o) => o
      case Invalid(errors) => sys.error(s"Expected valid but was invalid: ${errors.toNonEmptyList}")
    }
  }

  def expectInvalid[T](r: ValidatedNec[T, _]): Seq[T] = {
    r match {
      case Valid(_) => sys.error("Expected invalid but was valid")
      case Invalid(errors) => errors.toNonEmptyList.toList
    }
  }

}
