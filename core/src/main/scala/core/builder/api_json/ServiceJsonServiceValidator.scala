package builder.api_json

import io.apibuilder.apidoc.spec.v0.models.Service
import io.apibuilder.apidoc.spec.v0.models.json._
import lib.ServiceValidator
import play.api.libs.json.{Json, JsError, JsSuccess}
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import scala.util.{Failure, Success, Try}

case class ServiceJsonServiceValidator(
  json: String
) extends ServiceValidator[Service] {

  def validate(): Either[Seq[String], Service] = {
    Try(Json.parse(json)) match {
      case Success(js) => {
        js.validate[Service] match {
          case e: JsError => {
            Left(Seq("Not a valid service.json document: " + e.toString))
          }
          case s: JsSuccess[Service] => {
            Right(s.get)
          }
        }
      }

      case Failure(ex) => ex match {
        case e: JsonParseException => {
          Left(Seq("Invalid JSON: " + e.getMessage))
        }
        case e: JsonProcessingException => {
          Left(Seq("Invalid JSON: " + e.getMessage))
        }
      }
    }
  }

}
