package controllers

import db.ItemsDao
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

object Items extends Controller with Items

trait Items {
  this: Controller =>

  def getSearchByOrg(
    orgKey: String,
    q: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val items = ItemsDao.findAll(
      orgKey = Some(orgKey),
      q = q,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(items))
  }

}
