package controllers

import db.ItemsDao
import io.apibuilder.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Items @Inject() (
  itemsDao: ItemsDao
) extends Controller {

  def get(
    q: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val items = itemsDao.findAll(
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
    itemsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(item) => Ok(Json.toJson(item))
    }
  }

}
