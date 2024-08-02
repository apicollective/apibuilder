package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.{AppSortBy, ApplicationForm, MoveForm, SortOrder}
import io.apibuilder.api.v0.models.json._
import db._

import javax.inject.{Inject, Singleton}
import lib.Validation
import models.ApplicationsModel
import play.api.mvc._
import play.api.libs.json._

import java.util.UUID

@Singleton
class Applications @Inject() (
                               val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                               applicationsDao: InternalApplicationsDao,
                               versionsDao: InternalVersionsDao,
                               model: ApplicationsModel
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
  ): Action[AnyContent] = Identified { request =>
    val applications = applicationsDao.findAll(
      request.authorization,
      orgKey = Some(orgKey),
      name = name,
      key = key,
      guid = guid,
      hasVersion = hasVersion,
      limit = Some(limit),
      offset = offset,
      sorting = sorting,
      ordering = ordering
    )
    Ok(Json.toJson(model.toModels(applications)))
  }

  def post(orgKey: String): Action[JsValue] = Identified(parse.json) { request =>
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
              Ok(Json.toJson(model.toModel(app)))
            }
            case errors => {
              Conflict(Json.toJson(errors))
            }
          }
        }
      }
    }
  }

  def putByApplicationKey(orgKey: String, applicationKey: String): Action[JsValue] = Identified(parse.json) { request =>
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
                  Ok(Json.toJson(model.toModel(app)))
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

  def deleteByApplicationKey(orgKey: String, applicationKey: String): Action[AnyContent] = Identified { request =>
    withOrgMember(request.user, orgKey) { _ =>
      applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey).foreach { application =>
        applicationsDao.softDelete(request.user, application)
      }
      NoContent
    }
  }

  def postMoveByApplicationKey(orgKey: String, applicationKey: String): Action[JsValue] = Identified(parse.json) { request =>
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
              applicationsDao.move(request.user, app, form) match {
                case Valid(updatedApp) => {
                  Ok(Json.toJson(model.toModel(updatedApp)))
                }
                case Invalid(errors) => {
                  Conflict(Json.toJson(errors.toNonEmptyList.toList))
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
  ): Action[AnyContent] = Anonymous { request =>
    applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey) match {
      case None => NotFound
      case Some(application) => {
        val metadata = versionsDao.findAllVersions(
          request.authorization,
          applicationGuid = Some(application.guid),
          limit = Some(limit),
          offset = offset
        )
        Ok(Json.toJson(metadata))
      }
    }
  }

  def getMetadataAndVersionsAndLatestTxtByApplicationKey(
    orgKey: String,
    applicationKey: String,
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { request =>
    applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey) match {
      case None => NotFound
      case Some(application) => {
        versionsDao.findAllVersions(
          request.authorization,
          applicationGuid = Some(application.guid),
          limit = Some(1)
        ).headOption match {
          case None => NotFound
          case Some(v) => Ok(v.version)
        }
      }
    }
  }

}
