package helpers

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec

trait ValidatedTestHelpers {

  def expectValid[S, T](value: ValidatedNec[S, T]): T = {
    value match {
      case Invalid(e) => sys.error(s"Expected valid but got: ${e.toNonEmptyList.toList.mkString(", ")}")
      case Valid(v) => v
    }
  }

  def expectInvalid[T, U](r: ValidatedNec[T, U]): Seq[T] = {
    r match {
      case Valid(_) => sys.error("Expected invalid but was valid")
      case Invalid(errors) => errors.toNonEmptyList.toList
    }
  }

}
