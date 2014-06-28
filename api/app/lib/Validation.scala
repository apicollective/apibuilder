package lib

import play.api.libs.json.Json

case class ValidationError(code: String, message: String)

object ValidationError {

  implicit val validationErrorWrites = Json.writes[ValidationError]

}

object Validation {

  private val ErrorCode = "validation_error"

  def error(message: String): Seq[ValidationError] = {
    errors(Seq(message))
  }

  def errors(messages: Seq[String]): Seq[ValidationError] = {
    messages.map { msg => ValidationError(ErrorCode, msg) }
  }

}
