package builder.api_json

import cats.data.ValidatedNec
import cats.implicits._
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import lib.ServiceValidator
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.{Failure, Success, Try}

object ServiceJsonServiceValidator extends ServiceValidator[Service] {

  def validate(rawInput: String): ValidatedNec[String, Service] = {
    Try(Json.parse(rawInput)) match {
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
