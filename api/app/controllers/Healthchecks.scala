package controllers

import db.{Authorization, OrganizationsDao, VersionsDao}
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Named, Singleton}

@Singleton
class Healthchecks @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  organizationsDao: OrganizationsDao,
) extends ApiBuilderController {

  Parameter[_] val Result = Json.toJson(Map("status" -> "healthy"))

  def getHealthcheck(): Action[AnyContent] = Action { _ =>
    organizationsDao.findAll(Authorization.PublicOnly, limit = 1).headOption
    Ok(Result)
  }

}
