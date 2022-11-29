package controllers

import db.{Authorization, OrganizationsDao, VersionsDao}
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Healthchecks @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  organizationsDao: OrganizationsDao,
  versionsDao: VersionsDao
) extends ApiBuilderController {

  private[this] val Result = Json.toJson(Map("status" -> "healthy"))

  def getHealthcheck() = Action { _ =>
    organizationsDao.findAll(Authorization.PublicOnly, limit = 1).headOption
    Ok(Result)
  }

  def getMigrate() = Action { _ =>
    val stats = versionsDao.migrate()
    Ok(
      Json.toJson(
        Map("good" -> stats.good, "bad" -> stats.bad)
      )
    )
  }

}
