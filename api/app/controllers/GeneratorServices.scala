package controllers

import db.generators.ServicesDao
import lib.Validation
import com.bryzek.apidoc.api.v0.models.{User, GeneratorServiceForm}
import com.bryzek.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

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

            // Now to the initial update
            Try(actors.GeneratorServiceActor.sync(service)) match {
              case Success(_) => Ok(Json.toJson(service))
              case Failure(ex) => {
                ServicesDao.softDelete(request.user, service)
                Conflict(Json.toJson(Validation.error(s"Failed to fetch generators from service: ${ex.getMessage}")))
              }
            }
            
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

