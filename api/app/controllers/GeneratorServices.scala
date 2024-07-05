package controllers

import db.generators.ServicesDao
import lib.Validation
import io.apibuilder.api.v0.models.GeneratorServiceForm
import io.apibuilder.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import javax.inject.{Inject, Singleton}
import _root_.util.GeneratorServiceUtil

import scala.util.{Failure, Success, Try}

@Singleton
class GeneratorServices @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  servicesDao: ServicesDao,
  generatorServiceUtil: GeneratorServiceUtil
) extends ApiBuilderController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    guid: Option[UUID],
    uri: Option[String],
    generatorKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
    val services = servicesDao.findAll(
      request.authorization,
      guid = guid,
      uri = uri,
      generatorKey = generatorKey,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(services))
  }

  def getByGuid(guid: UUID) = Anonymous { request =>
    servicesDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(service) => Ok(Json.toJson(service))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[GeneratorServiceForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GeneratorServiceForm] => {
        val form = s.get
        servicesDao.validate(form) match {
          case Nil => {
            val service = servicesDao.create(request.user, form)

            // Now try to do the initial update; if it fails we delete the generator service.
            // TODO: Refactor so we can validate w/out creating first.
            Try(generatorServiceUtil.sync(service)) match {
              case Success(_) => Ok(Json.toJson(service))
              case Failure(ex) => {
                servicesDao.softDelete(request.user, service)
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
  ) = Identified.async {: _* request =>
    servicesDao.findByGuid(request.authorization, guid) match {
      case None => {
        NotFound
      }
      case Some(service) => {
        // TODO: Generalize permission check
        if (service.audit.createdBy.guid == request.user.guid) {
          servicesDao.softDelete(request.user, service)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}

