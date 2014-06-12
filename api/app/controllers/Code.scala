package controllers

import scala.util.Try

import play.api.mvc._
import play.api.libs.json._

import core.generator._
import apidoc.models
import db.DetailedVersion
import db.VersionDao

object Code extends Controller {
  def putByVersionguidAndTarget(versionGuid: String, target: String) = Action { request =>
    generator(target) match {
      case None => NotFound(s"No generator exists for target[$target]")
      case Some(f) => {
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

  private def generator(target: String): Option[DetailedVersion => models.Code] = Try {
    val source: String => String = target match {
      case "ruby-client" => RubyGemGenerator.apply
      case "play-2.2-routes" => Play2RouteGenerator.apply
      case "play-2.2-client" => Play2ClientGenerator.apply
      case "play-2.2-json" => Play2Models.apply
      case "scalacheck-generators" => ScalaCheckGenerators.apply
      case "scala-models" => ScalaCaseClasses.apply
    }

    { version: DetailedVersion =>
      new models.Code(version, target, source(version.json))
    }
  }.toOption
}
