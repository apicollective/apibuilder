package controllers

import db.VersionsDao
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Migrate extends Controller {

  def versions() = Action { request =>
    val result = VersionsDao.migrate()
    Ok(Json.toJson(result))
  }

}
