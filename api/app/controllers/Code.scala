package controllers

import play.api.mvc._
import play.api.libs.json._

import core.generator._
import apidoc.models
import db.DetailedVersion
import db.VersionDao

object Code extends Controller {
  def getByVersionAndTarget(versionGuid: String, target: String) = Action { request =>
    generator(target) match {
      case None => NotFound(
        Json.obj(
          "code" -> "invalid_target",
          "message" -> s"'$target' is not a valid target.",
          "data" -> Json.obj(
            "valid_targets "-> JsArray(
              Target.implemented.map(JsString(_))))))

      case Some(f) => {
        versionDetails(versionGuid) match {
          case None => NotFound(
            Json.obj(
              "code" -> "invalid_version",
              "message" -> s"No service exists for version[$versionGuid]"))
          case Some(details) => {
            val code: models.Code = f(details)
            Ok(Json.toJson(code))
          }
        }
      }
    }
  }

  private def versionDetails(guid: String): Option[DetailedVersion] = {
    VersionDao.findAll(guid = Some(guid)).headOption.flatMap(VersionDao.getDetails)
  }

  private def generator(target: String): Option[DetailedVersion => models.Code] = {
    Target.generator.lift(target).map { source =>
      { version: DetailedVersion =>
        new models.Code(version, target, source(version.json))
      }
    }
  }
}
