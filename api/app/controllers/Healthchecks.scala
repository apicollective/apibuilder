package controllers

import db.OrganizationDao
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Healthchecks extends Controller {

  private val Result = Json.toJson(Map("status" -> "healthy"))

  def get() = Action { request =>
    OrganizationDao.findAll(limit = 1).headOption
    Ok(Result)
  }

}
