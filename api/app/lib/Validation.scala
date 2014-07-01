package lib

import play.api.libs.json.Json
import play.api.libs.json.JsError

case class ValidationError(code: String, message: String)

object ValidationError {

  implicit val validationErrorWrites = Json.writes[ValidationError]

}

object Validation {

  private val InvalidJsonCode = "invalid_json"
  private val UserAuthorizationFailedCode = "user_authorization_failed"
  private val ErrorCode = "validation_error"

  def invalidJson(errors: JsError): Seq[ValidationError] = {
    Seq(ValidationError(InvalidJsonCode, errors.toString))
  }

  def userAuthorizationFailed(): Seq[ValidationError] = {
    Seq(ValidationError(UserAuthorizationFailedCode, "Email address and/or password did not match"))
  }

  def error(message: String): Seq[ValidationError] = {
    errors(Seq(message))
  }

  def errors(messages: Seq[String]): Seq[ValidationError] = {
    messages.map { msg => ValidationError(ErrorCode, msg) }
  }

}
