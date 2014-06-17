package controllers

import java.util.UUID

import play.api.mvc._
import play.api.libs.json._

import core.generator._
import apidoc.models
import apidoc.models.json._
import db.{ Version, VersionDao }

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

  private def versionDetails(guid: String): Option[Version] = {
    VersionDao.findAll(guid = Some(guid)).headOption
  }

  private def generator(target: String): Option[Version => models.Code] = {
    Target.generator.lift(target).map { source =>
      { v: Version =>
        val version = new models.Version(
          guid = UUID.fromString(v.guid),
          version = v.version,
          json = v.json
        )
        new models.Code(version, target, source(version.json))
      }
    }
  }
}
