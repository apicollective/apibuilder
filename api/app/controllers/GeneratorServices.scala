package controllers

import db.generators.ServicesDao
import lib.Validation
import com.gilt.apidoc.api.v0.models.{User, GeneratorServiceForm}
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object GeneratorServices extends Controller with GeneratorServices

trait GeneratorServices {
  this: Controller =>

  def get(
    guid: Option[UUID],
    uri: Option[String],
    generatorKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val services = ServicesDao.findAll(
      request.authorization,
      guid = guid,
      uri = uri,
      generatorKey = generatorKey,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(services))
  }

  def getByGuid(guid: _root_.java.util.UUID) = AnonymousRequest { request =>
    ServicesDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(service) => Ok(Json.toJson(service))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[GeneratorServiceForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GeneratorServiceForm] => {
        val form = s.get
        ServicesDao.validate(form) match {
          case Nil => {
            val service = ServicesDao.create(request.user, form)
            Ok(Json.toJson(service))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(
    guid: UUID
  ) = Authenticated(parse.json) { request =>
    ServicesDao.findByGuid(request.authorization, guid).map { service =>
      ServicesDao.softDelete(request.user, service)
    }
    NoContent
  }

}

