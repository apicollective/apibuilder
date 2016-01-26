package controllers

import db.{Authorization, OrganizationsDao, VersionsDao}
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Healthchecks extends Controller {

  private[this] val Result = Json.toJson(Map("status" -> "healthy"))

  def getHealthcheck() = Action { request =>
    OrganizationsDao.findAll(Authorization.PublicOnly, limit = 1).headOption
    Ok(Result)
  }

  def getMigrate() = Action { request =>
    val result = VersionsDao.migrate()
    Ok(Json.toJson(result))
  }

}
