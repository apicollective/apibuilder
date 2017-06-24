package controllers

import db.{Authorization, WatchesDao, FullWatchForm}
import lib.Validation
import io.apibuilder.apidoc.api.v0.models.{User, Watch, WatchForm}
import io.apibuilder.apidoc.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Watches @Inject() (
  watchesDao: WatchesDao
) extends Controller {

  def get(
    guid: Option[UUID],
    userGuid: Option[java.util.UUID],
    organizationKey: Option[String],
    applicationKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
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

  def getByGuid(guid: UUID) = Authenticated { request =>
    watchesDao.findByUserAndGuid(request.user, guid) match {
      case None => NotFound
      case Some(watch) => Ok(Json.toJson(watch))
    }
  }

  def getCheck(
    userGuid: scala.Option[_root_.java.util.UUID],
    organizationKey: String,
    applicationKey: String
  ) = Authenticated { request =>
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

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[WatchForm] match {
      case e: JsError => {
        BadRequest(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[WatchForm] => {
        val form = FullWatchForm(request.user, s.get)
        form.validate match {
          case Nil => {
            val watch = watchesDao.upsert(request.user, form)
            Created(Json.toJson(watch))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    watchesDao.findByUserAndGuid(request.user, guid).map { watch =>
      watchesDao.softDelete(request.user, watch)
    }
    NoContent
  }

}
