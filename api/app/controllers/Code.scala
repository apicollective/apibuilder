package controllers

import scala.util.{Try, Success, Failure}

import play.api.mvc._
import play.api.libs.json._

import core.generator._
import apidoc.models
import db.DetailedVersion
import db.VersionDao

object Code extends Controller {
  def getByVersionAndTarget(versionGuid: String, target: String) = Action { request =>
    generator(target) match {
      case Failure(_) => NotFound(s"No generator exists for target[$target]")
      case Success(f) => {
        versionDetails(versionGuid) match {
          case None => NotFound(s"No service exists for version[$versionGuid]")
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

  private def generator(target: String): Try[DetailedVersion => models.Code] = {
    Target.generator(target).map { source =>
      { version: DetailedVersion =>
        new models.Code(version, target, source(version.json))
      }
    }
  }
}
