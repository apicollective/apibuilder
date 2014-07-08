package controllers

import java.util.UUID
import lib.Validation
import core.generator.Target

import play.api.mvc._
import play.api.libs.json._

import db.{ Version, VersionDao }

object Code extends Controller {

  case class Code(
    target: String,
    source: String
  )

  object Code {

    implicit val codeWrites = Json.writes[Code]

  }

  def getByOrgKeyAndServiceKeyAndVersionAndTargetName(orgKey: String, serviceKey: String, version: String, targetName: String) = Authenticated { request =>
    VersionDao.findVersion(request.user, orgKey, serviceKey, version) match {

      case None => NotFound

      case Some(v: Version) => {
        generator(targetName) match {
          case None => {
            Conflict(Json.toJson(Validation.error(s"Invalid target[$targetName]. Must be one of: ${Target.implemented.mkString(" ")}")))
          }

          case Some(generator) => {
            val code = generator(v)
            Ok(Json.toJson(code))
          }
        }
      }
    }
  }

  private def generator(target: String): Option[Version => Code] = {
    Target.generator.lift(target).map { source =>
      { v: Version =>
        new Code(target, source(v.json))
      }
    }
  }

}
