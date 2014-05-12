package lib

import play.api.libs.json.Json

case class Validation(name: String, messages: Seq[String])

object Validation {

  implicit val validationWrites = Json.writes[Validation]

  private val Error = "error"

  def error(message: String): Validation = {
    Validation(Error, Seq(message))
  }

  def errors(messages: Seq[String]): Validation = {
    Validation(Error, messages)
  }

}
