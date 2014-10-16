package lib

import play.api.libs.json.Json
import play.api.libs.json.JsError

case class ValidationError(code: String, message: String)

object ValidationError {

  implicit val validationErrorWrites = Json.writes[ValidationError]

}

object Validation {

  private val InvalidJsonCode = "invalid_json"
  private val InvalidNameCode = "invalid_name"
  private val UserAuthorizationFailedCode = "user_authorization_failed"
  private val ErrorCode = "validation_error"
  private val ServerError = "server_error"

  def invalidJson(errors: JsError): Seq[ValidationError] = {
    Seq(ValidationError(InvalidJsonCode, errors.toString))
  }

  def invalidName(): Seq[ValidationError] = {
    Seq(ValidationError(InvalidNameCode, "Package name is not valid. Must be a dot separated list of valid names (start wtih letter, contains only a-z, A-Z, 0-9 and _ characters"))
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

  def serverError(error: String = "Internal Server Error"): Seq[ValidationError] = {
    Seq(ValidationError(ServerError, error))
  }

}
