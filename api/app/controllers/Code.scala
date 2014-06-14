package controllers

import java.util.UUID

import play.api.mvc._
import play.api.libs.json._

import core.generator._
import apidoc.models
import apidoc.models.json._
import db.DetailedVersion
import db.VersionDao

object Code extends Controller {
  def getByVersionAndTarget(versionGuid: String, target: String) = Action { request =>
    generator(target) match {
      case None => NotFound(
        Json.toJson(
          new models.CodeError(
            code = "invalid_target",
            message = s"'$target' is not a valid target.",
            validTargets = Target.implemented)))

      case Some(f) => {
        versionDetails(versionGuid) match {
          case None => NotFound(
            Json.toJson(
              new models.CodeError(
                code = "invalid_version",
                message = s"No service exists for version[$versionGuid]")))

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
      { dv: DetailedVersion =>
        val version = new models.Version(
          guid = UUID.fromString(dv.guid),
          version = dv.version,
          json = dv.json
        )
        new models.Code(version, target, source(version.json))
      }
    }
  }
}
