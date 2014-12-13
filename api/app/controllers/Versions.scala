package controllers

import com.gilt.apidoc.models.{Organization, User, Version, Visibility}
import com.gilt.apidoc.models.json._
import lib.Validation
import core.ServiceDescriptionValidator
import db.{Authorization, OrganizationsDao, ServicesDao, ServiceForm, VersionsDao, VersionForm}
import play.api.mvc._
import play.api.libs.json._

object Versions extends Controller {

  def getByOrgKeyAndServiceKey(orgKey: String, serviceKey: String, limit: Long = 25, offset: Long = 0) = AnonymousRequest { request =>
    val versions = ServicesDao.findByOrganizationKeyAndServiceKey(Authorization(request.user), orgKey, serviceKey).map { service =>
      VersionsDao.findAll(
        Authorization(request.user),
        serviceGuid = Some(service.guid),
        limit = limit,
        offset = offset
      )
    }.getOrElse(Seq.empty)
    Ok(Json.toJson(versions))
  }

  def getByOrgKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String) = AnonymousRequest { request =>
    VersionsDao.findVersion(Authorization(request.user), orgKey, serviceKey, version) match {
      case None => NotFound
      case Some(v: Version) => Ok(Json.toJson(v))
    }
  }

  def putByOrgKeyAndServiceKeyAndVersion(
    orgKey: String,
    serviceKey: String,
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
            val validator = ServiceDescriptionValidator(form.json)

            if (validator.isValid) {
              val visibility = form.visibility.getOrElse(Visibility.Organization)

              val service = ServicesDao.findByOrganizationKeyAndServiceKey(Authorization.User(request.user.guid), org.key, serviceKey).getOrElse {
                val serviceForm = ServiceForm(
                  name = validator.serviceDescription.get.name,
                  description = None,
                  visibility = visibility
                )
                ServicesDao.create(request.user, org, serviceForm, Some(serviceKey))
              }

              if (service.visibility != visibility) {
                ServicesDao.update(request.user, service.copy(visibility = visibility))
              }

              val resultingVersion = VersionsDao.findByServiceAndVersion(Authorization(Some(request.user)), service, version) match {
                case None => VersionsDao.create(request.user, service, version, form.json)
                case Some(existing: Version) => VersionsDao.replace(request.user, existing, service, form.json)
              }

              Ok(Json.toJson(resultingVersion))

            } else {
              Conflict(Json.toJson(Validation.errors(validator.errors)))
            }
          }
        }
      }
    }
  }

  def deleteByOrgKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String) = Authenticated { request =>
    val auth = Authorization.User(request.user.guid)
    OrganizationsDao.findByKey(auth, orgKey) map { org =>
      request.requireAdmin(org)
      VersionsDao.findVersion(auth, orgKey, serviceKey, version).map { version =>
        VersionsDao.softDelete(request.user, version)
      }
    }
    NoContent
  }

}
