package lib

import io.apibuilder.api.json.v0.models.ApiJson
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.api.v0.models.{Original, OriginalForm, OriginalType}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

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
    Try(Json.parse(trimmed)) match {
      case Success(o: JsObject) => {
        if (o.asOpt[ApiJson].isDefined) {
          Some(OriginalType.ApiJson)
        } else if (o.asOpt[Service].isDefined) {
          Some(OriginalType.ServiceJson)
        } else if ((o \ "swagger").asOpt[JsString].isDefined) {
          Some(OriginalType.Swagger)
        } else {
          guessApiOrServiceJson(o)
        }
      }
      case _ => {
        if (trimmed.indexOf("protocol ") >= 0 || trimmed.indexOf("@namespace") >= 0) {
          Some(OriginalType.AvroIdl)
        } else if (trimmed.contains("swagger:")) {
          Some(OriginalType.Swagger)
        } else {
          None
        }
      }
    }
  }

  private def guessApiOrServiceJson(o: JsObject): Option[OriginalType] = {
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
