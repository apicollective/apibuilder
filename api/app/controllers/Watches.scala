package controllers

import cats.data.Validated.{Invalid, Valid}
import db.WatchesDao
import io.apibuilder.api.v0.models.WatchForm
import io.apibuilder.api.v0.models.json._
import lib.Validation
import models.WatchesModel
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Watches @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  watchesDao: WatchesDao,
  model: WatchesModel,
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    userGuid: Option[java.util.UUID],
    organizationKey: Option[String],
    applicationKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    val watches = watchesDao.findAll(
      request.authorization,
      guid = guid,
      userGuid = userGuid,
      organizationKey =  organizationKey,
      applicationKey = applicationKey,
      limit = Some(limit),
      offset = offset
    )
    Ok(Json.toJson(model.toModels(watches)))
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    watchesDao.findByGuid(request.authorization, guid).flatMap(model.toModel) match {
      case None => NotFound
      case Some(watch) => Ok(Json.toJson(watch))
    }
  }

  def getCheck(
    userGuid: scala.Option[_root_.java.util.UUID],
    organizationKey: String,
    applicationKey: String
  ): Action[AnyContent] = Identified { request =>
    watchesDao.findAll(
      request.authorization,
      userGuid = userGuid,
      organizationKey =  Some(organizationKey),
      applicationKey = Some(applicationKey),
      limit = Some(1)
    ).headOption match {
      case None => Ok(Json.toJson(false))
      case Some(_) => Ok(Json.toJson(true))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[WatchForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[WatchForm] => {
        watchesDao.upsert(request.authorization, request.user, s.get) match {
          case Invalid(errors) => Conflict(Json.toJson(errors.toNonEmptyList.toList))
          case Valid(watch) => {
            Created(Json.toJson(
              model.toModel(watch).getOrElse {
                sys.error("Failed to create watch")
              }
            ))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    watchesDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(watch) => {
        watchesDao.softDelete(request.user, watch)
        NoContent
      }
    }
  }

}
