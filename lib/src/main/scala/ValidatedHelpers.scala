package lib

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import cats.data.{NonEmptyChain, ValidatedNec}

trait ValidatedHelpers {
  def sequence(all: Iterable[ValidatedNec[String, Any]]): ValidatedNec[String, Unit] = {
    all.toSeq.sequence.map(_ => ())
  }

  def formatErrors(value: ValidatedNec[String, Any]): String = {
    value match {
      case Invalid(errors) => formatErrors(errors)
      case Valid(_) => ""
    }
  }

  private[this] def formatErrors(errors: NonEmptyChain[String]): String = {
    errors.toNonEmptyList.toList.mkString(", ")
  }

  def addPrefixToError[T](prefix: String, value: ValidatedNec[String, T]): ValidatedNec[String, T] = {
    value match {
      case Invalid(errors) => s"$prefix: ${formatErrors(errors)}".invalidNec
      case Valid(t) => Valid(t)
    }
  }
}