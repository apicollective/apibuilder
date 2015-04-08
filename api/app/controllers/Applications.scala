package controllers

import com.gilt.apidoc.api.v0.models.ApplicationForm
import com.gilt.apidoc.api.v0.models.json._
import db.{Authorization, OrganizationsDao, ApplicationsDao}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object Applications extends Controller {

  def getByOrgKey(
    orgKey: String,
    name: Option[String],
    key: Option[String],
    hasVersion: Option[Boolean],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val applications = ApplicationsDao.findAll(
      Authorization(request.user),
      orgKey = Some(orgKey),
      name = name,
      key = key,
      hasVersion = hasVersion,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(applications))
  }

  def postByOrgKey(orgKey: String) = Authenticated(parse.json) { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
        request.requireAdmin(org)

        request.body.validate[ApplicationForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[ApplicationForm] => {
            val form = s.get
            ApplicationsDao.validate(org, form) match {
              case Nil => {
                val app = ApplicationsDao.create(request.user, org, form)
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

  def putByOrgKeyAndApplicationKey(orgKey: String, applicationKey: String) = Authenticated(parse.json) { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
        request.requireAdmin(org)

        request.body.validate[ApplicationForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[ApplicationForm] => {
            val form = s.get
            ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.User(request.user.guid), org.key, applicationKey) match {
              case None => Conflict(Json.toJson(Validation.error(s"application[$applicationKey] not found or inaccessible")))
              case Some(existing) => {
                ApplicationsDao.validate(org, form, Some(existing)) match {
                  case Nil => {
                    val app = ApplicationsDao.update(request.user, existing, form)
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

  def deleteByOrgKeyAndApplicationKey(orgKey: String, applicationKey: String) = Authenticated { request =>
    OrganizationsDao.findByKey(Authorization.User(request.user.guid), orgKey) map { org =>
      request.requireMember(org)
      ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.User(request.user.guid), orgKey, applicationKey).map { application =>
        ApplicationsDao.softDelete(request.user, application)
      }
    }
    NoContent
  }

}
