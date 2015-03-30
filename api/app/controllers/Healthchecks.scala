package controllers

import db.{Authorization, OrganizationsDao, VersionsDao}
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Healthchecks extends Controller {

  private val Result = Json.toJson(Map("status" -> "healthy"))

  def getInternalAndHealthcheck() = Action { request =>
    OrganizationsDao.findAll(Authorization.PublicOnly, limit = 1).headOption
    Ok(Result)
  }

  def getInternalAndMigrate() = Action { request =>
    val result = VersionsDao.migrate()
    Ok(Json.toJson(result))
  }

}
