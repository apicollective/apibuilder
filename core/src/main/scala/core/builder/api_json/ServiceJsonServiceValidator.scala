package builder.api_json

import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import lib.ServiceValidator
import play.api.libs.json.{Json, JsError, JsSuccess}
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import scala.util.{Failure, Success, Try}

case class ServiceJsonServiceValidator(
  json: String
) extends ServiceValidator[Service] {

  def validate(): ValidatedNec[String, Service] = {
    Try(Json.parse(json)) match {
      case Success(js) => {
        js.validate[Service] match {
          case e: JsError => {
            ("Not a valid service.json document: " + e.toString).invalidNec
          }
          case s: JsSuccess[Service] => {
            s.get.validNec
          }
        }
      }

      case Failure(ex) => ex match {
        case e: JsonParseException => {
          ("Invalid JSON: " + e.getMessage).invalidNec
        }
        case e: JsonProcessingException => {
          ("Invalid JSON: " + e.getMessage).invalidNec
        }
      }
    }
  }

}
