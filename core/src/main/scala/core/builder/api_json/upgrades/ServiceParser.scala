package builder.api_json.upgrades

import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}

import javax.inject.Inject
import scala.util.{Failure, Success, Try}

class ServiceParser @Inject() () {

  def fromString(js: String): ValidatedNec[String, Service] = {
    Try {
      Json.parse(js)
    } match {
      case Success(j) => fromJson(j)
      case Failure(ex) => s"Invalid JSON: ${ex.getMessage}".invalidNec
    }
  }

  def fromJson(js: JsValue): ValidatedNec[String, Service] = {
    js match {
      case o: JsObject => {
        upgrade(o).validate[Service] match {
          case JsSuccess(s, _) => s.validNec
          case JsError(errors) => errors.map { case (path, msg) =>
            s"${path.path.map(_.toJsonString).mkString("/")}: ${msg.flatMap(_.messages).mkString(", ")}"
          }.mkString(", ").invalidNec
        }
      }
      case _ => "Can only parse service from a JSON Object".invalidNec
    }
  }

  private[this] val DefaultVersion: JsObject = Json.obj("apidoc" -> Json.obj("version" -> "1.0"))
  private[this] def upgrade(js: JsObject): JsObject = {
    DefaultVersion ++ js
  }

}
