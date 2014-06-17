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

  def getByVersionAndTarget(versionGuid: String, target: String) = Action { request =>
    VersionDao.findAll(guid = Some(versionGuid)).headOption match {
      case None => NotFound

      case Some(v: Version) => {
        generator(target) match {
          case None => {
            val error = Validation.error(s"Invalid target[$target]. Must be one of: ${Target.implemented.mkString(" ")}")
            Conflict(Json.toJson(error))
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
