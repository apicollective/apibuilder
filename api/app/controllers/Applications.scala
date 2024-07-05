package controllers

import io.apibuilder.api.v0.models.{ApplicationForm, AppSortBy, MoveForm, SortOrder}
import io.apibuilder.api.v0.models.json._
import db._
import javax.inject.{Inject, Singleton}

import lib.Validation
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Applications @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  applicationsDao: ApplicationsDao,
  versionsDao: VersionsDao
) extends ApiBuilderController {

  def get(
    orgKey: String,
    name: Option[String],
    guid: Option[UUID],
    key: Option[String],
    hasVersion: Option[Boolean],
    limit: Long = 25,
    offset: Long = 0,
    sorting: Option[AppSortBy],
    ordering: Option[SortOrder]
  ) = Identified.async {: _* request =>
    val applications = applicationsDao.findAll(
      request.authorization,
      orgKey = Some(orgKey),
      name = name,
      key = key,
      guid = guid,
      hasVersion = hasVersion,
      limit = limit,
      offset = offset,
      sorting = sorting,
      ordering = ordering
    )
    Ok(Json.toJson(applications))
  }

  def post(orgKey: String) = Identified(parse.json) { request =>
    withOrg(request.authorization, orgKey) { org =>
      request.body.validate[ApplicationForm] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ApplicationForm] => {
          val form = s.get
          applicationsDao.validate(org, form) match {
            case Nil => {
              val app = applicationsDao.create(request.user, org, form)
              Ok(Json.toJson(app))
            }
            case errors => {
              Conflict(Json.toJson(errors))
            }
          }
        }
      }
    }
  }

  def putByApplicationKey(orgKey: String, applicationKey: String) = Identified(parse.json) { request =>
    withOrg(request.authorization, orgKey) { org =>
      request.body.validate[ApplicationForm] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ApplicationForm] => {
          val form = s.get
          applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, org.key, applicationKey) match {
            case None => Conflict(Json.toJson(Validation.error(s"application[$applicationKey] not found or inaccessible")))
            case Some(existing) => {
              applicationsDao.validate(org, form, Some(existing)) match {
                case Nil => {
                  val app = applicationsDao.update(request.user, existing, form)
                  Ok(Json.toJson(app))
                }
                case errors => {
                  Conflict(Json.toJson(errors))
                }
              }
            }
          }
        }
      }
    }
  }

  def deleteByApplicationKey(orgKey: String, applicationKey: String) = Identified.async {: _* request =>
    withOrgMember(request.user, orgKey) { _ =>
      applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey).foreach { application =>
        applicationsDao.softDelete(request.user, application)
      }
      NoContent
    }
  }

  def postMoveByApplicationKey(orgKey: String, applicationKey: String) = Identified(parse.json) { request =>
    withOrg(request.authorization, orgKey) { org =>
      applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, org.key, applicationKey) match {
        case None => NotFound
        case Some(app) => {
          request.body.validate[MoveForm] match {
            case e: JsError => {
              Conflict(Json.toJson(Validation.invalidJson(e)))
            }
            case s: JsSuccess[MoveForm] => {
              val form = s.get
              applicationsDao.validateMove(request.authorization, app, form) match {
                case Nil => {
                  val updatedApp = applicationsDao.move(request.user, app, form)
                  Ok(Json.toJson(updatedApp))
                }
                case errors => {
                  Conflict(Json.toJson(errors))
                }
              }
            }
          }
        }
      }
    }
  }

  def getMetadataAndVersionsByApplicationKey(
    orgKey: String,
    applicationKey: String,
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
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

  def getMetadataAndVersionsAndLatestTxtByApplicationKey(
    orgKey: String,
    applicationKey: String,
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
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
