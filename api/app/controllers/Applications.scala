package controllers

import com.bryzek.apidoc.api.v0.models.{ApplicationForm, MoveForm}
import com.bryzek.apidoc.api.v0.models.json._
import db.{Authorization, OrganizationsDao, ApplicationsDao}
import javax.inject.{Inject, Singleton}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Applications @Inject() (
  applicationsDao: ApplicationsDao,
  organizationsDao: OrganizationsDao
) extends Controller {

  def get(
    orgKey: String,
    name: Option[String],
    guid: Option[UUID],
    key: Option[String],
    hasVersion: Option[Boolean],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val applications = applicationsDao.findAll(
      request.authorization,
      orgKey = Some(orgKey),
      name = name,
      key = key,
      guid = guid,
      hasVersion = hasVersion,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(applications))
  }

  def post(orgKey: String) = Authenticated(parse.json) { request =>
    organizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
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
  }

  def putByApplicationKey(orgKey: String, applicationKey: String) = Authenticated(parse.json) { request =>
    organizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
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
  }

  def deleteByApplicationKey(orgKey: String, applicationKey: String) = Authenticated { request =>
    organizationsDao.findByKey(request.authorization, orgKey) map { org =>
      request.requireMember(org)
      applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey).map { application =>
        applicationsDao.softDelete(request.user, application)
      }
    }
    NoContent
  }

  def postMoveByApplicationKey(orgKey: String, applicationKey: String) = Authenticated(parse.json) { request =>
    organizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
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
  }
}
