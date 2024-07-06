package controllers

import db.WatchesDao
import lib.Validation
import io.apibuilder.api.v0.models.WatchForm
import io.apibuilder.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Watches @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  watchesDao: WatchesDao
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
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(watches))
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    watchesDao.findByGuid(request.authorization, guid) match {
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
      limit = 1
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
        watchesDao.validate(request.authorization, s.get) match {
          case Left(errors) => Conflict(Json.toJson(errors))
          case Right(validatedForm) => {
            val watch = watchesDao.upsert(request.user, validatedForm)
            Created(Json.toJson(watch))
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
