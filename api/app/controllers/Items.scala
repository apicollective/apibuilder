package controllers

import db.ItemsDao
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Items extends Controller with Items

trait Items {
  this: Controller =>

  def get(
    q: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val items = ItemsDao.findAll(
      request.authorization,
      q = q,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(items))
  }

  def getByGuid(
    guid: UUID
  ) = AnonymousRequest { request =>
    ItemsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(item) => Ok(Json.toJson(item))
    }
  }

}
