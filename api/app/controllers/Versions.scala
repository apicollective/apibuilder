package controllers

import lib.Validation
import core.{ ServiceDescription, ServiceDescriptionValidator }
import db.{ Organization, OrganizationDao, ServiceDao, User, Version, VersionDao }
import play.api.mvc._
import play.api.libs.json.Json

object Versions extends Controller {

  def getByOrgKeyAndServiceKey(orgKey: String, serviceKey: String, limit: Int = 25, offset: Int = 0) = Authenticated { request =>
    val versions = findOrganizationByUserAndKey(request.user, orgKey).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, serviceKey).map { service =>
        VersionDao.findAll(service_guid = Some(service.guid),
                           limit = limit,
                           offset = offset)
      }
    }.getOrElse(Seq.empty)
    Ok(Json.toJson(versions))
  }

  def getByOrgKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String) = Authenticated { request =>
    getVersion(request.user, orgKey, serviceKey, version) match {
      case None => NotFound
      case Some(v: Version) => {
        Ok(Json.toJson(v))
      }
    }
  }

  def putByOrgKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String) = Authenticated(parse.json) { request =>
    findOrganizationByUserAndKey(request.user, orgKey) match {
      case None => {
        BadRequest(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }

      case Some(org: Organization) => {

        val validator = ServiceDescriptionValidator(request.body.toString)

        if (validator.isValid) {
          val service = ServiceDao.findByOrganizationAndKey(org, serviceKey).getOrElse {
            ServiceDao.create(request.user, org, validator.serviceDescription.get.name, Some(serviceKey))
          }

          VersionDao.findByServiceAndVersion(service, version) match {
            case None => VersionDao.create(request.user, service, version, request.body.toString)
            case Some(existing: Version) => VersionDao.replace(request.user, existing, service, request.body.toString)
          }

          NoContent

        } else {
          BadRequest(Json.toJson(Validation.errors(validator.errors)))

        }
      }
    }
  }

  def deleteByOrgKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String) = Authenticated { request =>
    getVersion(request.user, orgKey, serviceKey, version).map { version =>
      VersionDao.softDelete(request.user, version)
    }
    NoContent
  }


  private def getVersion(user: User, orgKey: String, serviceKey: String, version: String): Option[Version] = {
    findOrganizationByUserAndKey(user, orgKey).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, serviceKey).flatMap { service =>
        if (version == "latest") {
          VersionDao.findAll(service_guid = Some(service.guid), limit = 1).headOption
        } else {
          VersionDao.findByServiceAndVersion(service, version)
        }
      }
    }
  }

  private def findOrganizationByUserAndKey(user: User, orgKey: String): Option[Organization] = {
    // TODO: User authorization
    OrganizationDao.findAll(key = Some(orgKey), limit = 1).headOption
  }

}
