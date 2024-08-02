package controllers

import cats.data.Validated.{Invalid, Valid}
import db.generators.InternalGeneratorServicesDao
import lib.Validation
import io.apibuilder.api.v0.models.GeneratorServiceForm
import io.apibuilder.api.v0.models.json.*
import play.api.mvc.*
import play.api.libs.json.*

import java.util.UUID
import javax.inject.{Inject, Singleton}
import _root_.util.GeneratorServiceUtil
import models.GeneratorServicesModel

import scala.util.{Failure, Success, Try}

@Singleton
class GeneratorServices @Inject() (
                                    val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                    servicesDao: InternalGeneratorServicesDao,
                                    generatorServiceUtil: GeneratorServiceUtil,
                                    serviceModel: GeneratorServicesModel,
) extends ApiBuilderController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    guid: Option[UUID],
    uri: Option[String],
    generatorKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { request =>
    val services = servicesDao.findAll(
      guid = guid,
      uri = uri,
      generatorKey = generatorKey,
      limit = Some(limit),
      offset = offset
    )
    Ok(Json.toJson(serviceModel.toModels(services)))
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Anonymous { request =>
    servicesDao.findByGuid(guid) match {
      case None => NotFound
      case Some(service) => Ok(Json.toJson(serviceModel.toModel(service)))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[GeneratorServiceForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: GeneratorServiceForm, _) => {
        servicesDao.create(request.user, form) match {
          case Valid(service) => {
            // Now try to do the initial update; if it fails we delete the generator service.
            // TODO: Refactor so we can validate w/out creating first.
            Try(generatorServiceUtil.sync(service)) match {
              case Success(_) => Ok(Json.toJson(serviceModel.toModel(service)))
              case Failure(ex) => {
                servicesDao.softDelete(request.user, service)
                Conflict(Json.toJson(Validation.error(s"Failed to fetch generators from service: ${ex.getMessage}")))
              }
            }
          }
          case Invalid(errors) => {
            Conflict(Json.toJson(errors.toNonEmptyList.toList))
          }
        }
      }
    }
  }

  def deleteByGuid(
    guid: UUID
  ): Action[AnyContent] = Identified { request =>
    servicesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(service) => {
        // TODO: Generalize permission check
        if (service.db.createdByGuid == request.user.guid) {
          servicesDao.softDelete(request.user, service)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}

