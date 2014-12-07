package controllers

import db.{Authorization, WatchDao, FullWatchForm}
import lib.Validation
import com.gilt.apidoc.models.{User, Watch, WatchForm}
import com.gilt.apidoc.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Watches extends Controller with Watches

trait Watches {
  this: Controller =>

  def get(
    guid: Option[UUID],
    userGuid: Option[java.util.UUID],
    serviceKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val watchs = WatchDao.findAll(
      Authorization(Some(request.user)),
      guid = guid,
      userGuid = userGuid,
      serviceKey = serviceKey,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(watchs))
  }

  def getByGuid(guid: UUID) = Authenticated { request =>
    WatchDao.findByUserAndGuid(request.user, guid) match {
      case None => NotFound
      case Some(watch) => Ok(Json.toJson(watch))
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
            val watch = WatchDao.create(request.user, form)
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
    WatchDao.findByUserAndGuid(request.user, guid).map { watch =>
      WatchDao.softDelete(request.user, watch)
    }
    NoContent
  }

}
