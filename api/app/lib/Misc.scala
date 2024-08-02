package lib

import cats.data.ValidatedNec
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import io.apibuilder.api.v0.models.Error

object Misc {

  def validateEmail(email: String): ValidatedNec[Error, String] = {
    def err(msg: String) = Validation.singleError(msg).invalidNec
    val trimmed = email.trim

    if (!trimmed.contains("@")) {
      err("Email must have an '@' symbol")

    } else if (trimmed == "@") {
      err("Invalid Email: missing username and domain")

    } else if (trimmed.startsWith("@")) {
      err("Invalid Email: missing username")

    } else if (trimmed.endsWith("@")) {
      err("Invalid Email: missing domain")

    } else {
      trimmed.validNec
    }
  }

  def emailDomain(email: String): Option[String] = {
    email.trim.split("@").toList match {
      case _ :: domain :: Nil => {
        Some(domain.toLowerCase.trim).filter(_.nonEmpty)
      }
      case _ => None
    }
  }

}