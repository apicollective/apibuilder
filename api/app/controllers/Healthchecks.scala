package controllers

import db.{Authorization, OrganizationsDao, VersionsDao}
import javax.inject.{Inject, Named, Singleton}
import play.api._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Healthchecks @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  organizationsDao: OrganizationsDao,
  versionsDao: VersionsDao
) extends Controller {

  private[this] val Result = Json.toJson(Map("status" -> "healthy"))

  def getHealthcheck() = Action { request =>
    organizationsDao.findAll(Authorization.PublicOnly, limit = 1).headOption
    Ok(Result)
  }

  def getMigrate() = Action { request =>
    val result = versionsDao.migrate()
    Ok(Json.toJson(result))
  }

}
