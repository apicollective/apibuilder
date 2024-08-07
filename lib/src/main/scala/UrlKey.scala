package lib

import cats.implicits._
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec

object UrlKey {

  private val MinKeyLength = 3

  // Only want lower case letters and dashes
  private val Regexp1 = """([^0-9a-z\-\_])""".r

  // Turn multiple dashes into single dashes
  private val Regexp2 = """(\-+)""".r

  // Turn multiple underscores into single underscore
  private val Regexp3 = """(\_+)""".r

  private val RegexpLeadingSpaces = """^\-+""".r
  private val RegexpTrailingSpaces = """\-+$""".r

  def generate(value: String): String = {
    generate(format(value), 0)
  }

  @scala.annotation.tailrec
  private def generate(value: String, suffix: Int): String = {
    val key = if (suffix <= 0) { value } else { s"$value-1" }
    validate(key) match {
      case Nil => key
      case errors => generate(key, suffix + 1)
    }
  }

  def format(value: String): String = {
    RegexpTrailingSpaces.replaceAllIn(
      RegexpLeadingSpaces.replaceAllIn(
        Regexp3.replaceAllIn(
          Regexp2.replaceAllIn(
            Regexp1.replaceAllIn(value.toLowerCase.trim, m => "-"),
            m => "-"
          ), m => "_"
        ), m => ""),
      m => ""
    )
  }

  def validateNec(key: String, label: String = "Key"): ValidatedNec[String, Unit] = {
    val generated = UrlKey.format(key)
    if (key.length < MinKeyLength) {
      s"$label must be at least $MinKeyLength characters".invalidNec
    } else if (key != generated) {
      s"$label must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid ${label.toLowerCase} would be: $generated".invalidNec
    } else {
      ().validNec
    }
  }

  def validate(key: String, label: String = "Key"): Seq[String] = {
    validateNec(key, label) match {
      case Invalid(e) => e.toNonEmptyList.toList
      case Valid(_) => Nil
    }
  }

}
