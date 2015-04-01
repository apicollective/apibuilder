package lib

import com.gilt.apidoc.api.v0.models.{Original, OriginalForm, OriginalType}
import play.api.libs.json.{Json, JsString, JsObject}

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
      Json.parse(trimmed).asOpt[JsObject] match {
        case None => None
        case Some(o) => {
          (o \ "swagger").asOpt[JsString] match {
            case Some(v) => Some(OriginalType.SwaggerJson)
            case None => Some(OriginalType.ApiJson)
          }
        }
      }
    } else {
      None
    }
  }

}
