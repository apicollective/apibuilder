package lib

import cats.data.NonEmptyChain
import io.apibuilder.api.v0.models.Error
import play.api.libs.json.JsError

object Validation {

  private val InvalidJsonCode = "invalid_json"
  private val UserAuthorizationFailedCode = "user_authorization_failed"
  private val Unauthorized = "unauthorized"
  private val ErrorCode = "validation_error"
  private val ServerError = "server_error"

  def invalidJson(errors: JsError): Seq[Error] = {
    Seq(Error(InvalidJsonCode, errors.toString))
  }

  def invalidJsonDocument(): Seq[Error] = {
    Seq(Error(InvalidJsonCode, "Content is not valid JSON"))
  }

  def userAuthorizationFailed(): Seq[Error] = {
    Seq(Error(UserAuthorizationFailedCode, "Email address and/or password did not match"))
  }

  def unauthorized(message: String): Seq[Error] = {
    Seq(Error(Unauthorized, message))
  }

  def error(message: String): Seq[Error] = {
    errors(Seq(message))
  }

  def errors(messages: Seq[String]): Seq[Error] = {
    messages.map { msg => Error(ErrorCode, msg) }
  }

  def errors(messages: NonEmptyChain[String]): Seq[Error] = errors(messages.toNonEmptyList.toList)

  def serverError(error: String = "Internal Server Error"): Seq[Error] = {
    Seq(Error(ServerError, error))
  }

}
