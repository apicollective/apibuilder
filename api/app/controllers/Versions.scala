package controllers

import com.gilt.apidoc.models.{ApplicationForm, Organization, User, Version, VersionForm, Visibility}
import com.gilt.apidoc.models.json._
import lib.Validation
import core.ServiceValidator
import db.{ApplicationsDao, Authorization, OrganizationsDao, VersionsDao}
import play.api.mvc._
import play.api.libs.json._

object Versions extends Controller {

  def getByOrgKeyAndApplicationKey(orgKey: String, applicationKey: String, limit: Long = 25, offset: Long = 0) = AnonymousRequest { request =>
    val versions = ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization(request.user), orgKey, applicationKey).map { application =>
      VersionsDao.findAll(
        Authorization(request.user),
        applicationGuid = Some(application.guid),
        limit = limit,
        offset = offset
      )
    }.getOrElse(Seq.empty)
    Ok(Json.toJson(versions))
  }

  def getByOrgKeyAndApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = AnonymousRequest { request =>
    VersionsDao.findVersion(Authorization(request.user), orgKey, applicationKey, version) match {
      case None => NotFound
      case Some(v: Version) => Ok(Json.toJson(v))
    }
  }

  def putByOrgKeyAndApplicationKeyAndVersion(
    orgKey: String,
    applicationKey: String,
    version: String
  ) = Authenticated(parse.json) { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => {
        Conflict(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }

      case Some(org: Organization) => {

        request.body.validate[VersionForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[VersionForm] => {
            val form = s.get
            val validator = ServiceValidator(form.json.toString)
            validator.errors match {
              case Nil => {
                val visibility = form.visibility.getOrElse(Visibility.Organization)

                val application = ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.User(request.user.guid), org.key, applicationKey).getOrElse {
                  val form = ApplicationForm(
                    name = validator.serviceDescription.get.name,
                    description = None,
                    visibility = visibility
                  )
                  ApplicationsDao.create(request.user, org, form, Some(applicationKey))
                }

                if (application.visibility != visibility) {
                  ApplicationsDao.setVisibility(request.user, application, visibility)
                }

                val resultingVersion = VersionsDao.findByApplicationAndVersion(Authorization(Some(request.user)), application, version) match {
                  case None => VersionsDao.create(request.user, application, version, form.json)
                  case Some(existing: Version) => VersionsDao.replace(request.user, existing, application, form.json)
                }

                Ok(Json.toJson(resultingVersion))
              }
              case errors => {
                Conflict(Json.toJson(Validation.errors(validator.errors)))
              }
            }
          }
        }
      }
    }
  }

  def deleteByOrgKeyAndApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = Authenticated { request =>
    val auth = Authorization.User(request.user.guid)
    OrganizationsDao.findByKey(auth, orgKey) map { org =>
      request.requireAdmin(org)
      VersionsDao.findVersion(auth, orgKey, applicationKey, version).map { version =>
        VersionsDao.softDelete(request.user, version)
      }
    }
    NoContent
  }

}
