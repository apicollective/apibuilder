package controllers

import io.apibuilder.api.v0.models.json._
import db.{ApplicationsDao, VersionsDao}
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class ApplicationMetadata @Inject() (
  applicationsDao: ApplicationsDao,
  versionsDao: VersionsDao
) extends Controller {

  def getVersions(
    orgKey: String,
    applicationKey: String,
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey) match {
      case None => NotFound
      case Some(application) => {
        val versions = versionsDao.findAllVersions(
          request.authorization,
          applicationGuid = Some(application.guid),
          limit = limit,
          offset = offset
        )
        Ok(Json.toJson(versions))
      }
    }
  }

  def getVersionsAndLatestTxt(
    orgKey: String,
    applicationKey: String,
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey) match {
      case None => NotFound
      case Some(application) => {
        versionsDao.findAllVersions(
          request.authorization,
          applicationGuid = Some(application.guid),
          limit = 1
        ).headOption match {
          case None => NotFound
          case Some(v) => Ok(v.version)
        }
      }
    }
  }

}
