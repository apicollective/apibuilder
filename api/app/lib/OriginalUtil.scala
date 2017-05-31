package lib

import com.bryzek.apidoc.api.v0.models.{Original, OriginalForm, OriginalType}
import com.bryzek.apidoc.spec.v0.models.Service
import com.bryzek.apidoc.spec.v0.models.json._
import play.api.libs.json.{Json, JsString, JsObject, JsSuccess}
import scala.util.{Failure, Success, Try}

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
        Json.parse(trimmed).asOpt[JsObject] match {
          case None => None
          case Some(o) => {
            (o \ "swagger").asOpt[JsString] match {
              case Some(v) => Some(OriginalType.Swagger)
              case None => {
                o.validate[Service] match {
                  case JsSuccess(_, _) => Some(OriginalType.ServiceJson)
                  case _ => Some(OriginalType.ApiJson)
                }
              }
            }
          }
        }
      ) match {
        case Success(ot) => ot
        case Failure(e) => None
      }
    } else {
      None
    }
  }

}
