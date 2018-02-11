package controllers

import db.ItemsDao
import io.apibuilder.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Items @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  itemsDao: ItemsDao
) extends ApibuilderController {

  def get(
    q: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
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
  ) = Anonymous { request =>
    itemsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(item) => Ok(Json.toJson(item))
    }
  }

}
