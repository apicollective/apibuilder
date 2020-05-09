package lib

import io.apibuilder.api.json.v0.models.ApiJson
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.api.v0.models.{Original, OriginalForm, OriginalType}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import play.api.libs.json.{JsArray, JsObject, JsString, JsSuccess, Json}

import scala.util.{Success, Try}

object OriginalUtil {

  def toOriginal(form: OriginalForm): Original = {
    Original(
      `type` = form.`type`.getOrElse(
        guessType(form.data).getOrElse(OriginalType.ApiJson)
      ),
      data = form.data
    )
  }

  /**
    * Attempts to guess the type of original based on the data
    */
  def guessType(data: String): Option[OriginalType] = {
    val trimmed = data.trim
    if (trimmed.indexOf("protocol ") >= 0 || trimmed.indexOf("@namespace") >= 0) {
      Some(OriginalType.AvroIdl)
    } else if (trimmed.startsWith("{")) {
      Try(
        Json.parse(trimmed)
      ) match {
        case Success(o: JsObject) => {
          (o \ "swagger").asOpt[JsString] match {
            case Some(_) => Some(OriginalType.Swagger)
            case None => guessApiOrServiceJson(o)
          }
        }
        case _ => None
      }
    } else if (trimmed.contains("swagger:")) {
      Some(OriginalType.Swagger)
    } else {
      None
    }
  }

  def guessApiOrServiceJson(o: JsObject): Option[OriginalType] = {
    o.validate[Service] match {
      case JsSuccess(_, _) => Some(OriginalType.ServiceJson)
      case _ => {
        o.validate[ApiJson] match {
          case JsSuccess(_, _) => Some(OriginalType.ApiJson)
          case _ => {
            // service.json has these defined as array; api.json as maps
            val fields = Seq("enums", "interfaces", "unions", "models")

            val arrays = fields.flatMap { f => (o \ f).asOpt[JsArray] }
            val objects = fields.flatMap { f => (o \ f).asOpt[JsObject] }
            if (arrays.nonEmpty && objects.isEmpty) {
              Some(OriginalType.ServiceJson)
            } else if (arrays.isEmpty && objects.nonEmpty) {
              Some(OriginalType.ApiJson)
            } else {
              None
            }
          }
        }
      }
    }

  }

}
