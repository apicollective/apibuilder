package lib

import com.gilt.apidocspec.models.Error
import play.api.libs.json.Json
import play.api.libs.json.JsError

object Validation {

  private val InvalidJsonCode = "invalid_json"
  private val InvalidNameCode = "invalid_name"
  private val UserAuthorizationFailedCode = "user_authorization_failed"
  private val ErrorCode = "validation_error"
  private val ServerError = "server_error"

  def invalidJson(errors: JsError): Seq[Error] = {
    Seq(Error(InvalidJsonCode, errors.toString))
  }

  def invalidName(): Seq[Error] = {
    Seq(Error(InvalidNameCode, "Package name is not valid. Must be a dot separated list of valid names (start wtih letter, contains only a-z, A-Z, 0-9 and _ characters"))
  }

  def userAuthorizationFailed(): Seq[Error] = {
    Seq(Error(UserAuthorizationFailedCode, "Email address and/or password did not match"))
  }

  def error(message: String): Seq[Error] = {
    errors(Seq(message))
  }

  def errors(messages: Seq[String]): Seq[Error] = {
    messages.map { msg => Error(ErrorCode, msg) }
  }

  def serverError(error: String = "Internal Server Error"): Seq[Error] = {
    Seq(Error(ServerError, error))
  }

}
