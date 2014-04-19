package controllers

import core.{ ServiceDescription, ServiceDescriptionValidator }
import db.{ Organization, OrganizationDao, ServiceDao, User, Version, VersionDao }
import play.api.mvc._
import play.api.libs.json.Json

object Versions extends Controller {

  def getService(org: String, service: String, limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val versions = OrganizationDao.findByUserAndKey(request.user, org).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, service).map { service =>
        VersionDao.findAll(service_guid = service.guid,
                           limit = limit,
                           offset = offset)
      }
    }.getOrElse(Seq.empty)
    Ok(Json.toJson(versions))
  }

  def getServiceLatest(org: String, service: String) = Authenticated { request =>
    getVersion(request.user, org, service, "latest") match {
      case None => NotFound
      case Some(v: Version) => {
        val detailedVersion = VersionDao.getDetails(v).getOrElse {
          sys.error(s"Error fetching details for version[${v}]")
        }
        Ok(Json.toJson(detailedVersion))
      }
    }
  }

  def getServiceVersion(org: String, service: String, version: String) = Authenticated { request =>
    getVersion(request.user, org, service, version) match {
      case None => NotFound
      case Some(v: Version) => {
        val detailedVersion = VersionDao.getDetails(v).getOrElse {
          sys.error(s"Error fetching details for version[${v}]")
        }
        Ok(Json.toJson(detailedVersion))
      }
    }
  }

  def putServiceVersion(orgKey: String, serviceKey: String, version: String) = Authenticated(parse.json) { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => BadRequest(s"Organization[$orgKey] does not exist or you are not authorized to access it")
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
          BadRequest(validator.errors.mkString(", "))

        }
      }
    }
  }

  def deleteServiceVersion(org: String, service: String, version: String) = Authenticated { request =>
    getVersion(request.user, org, service, version).map { version =>
      VersionDao.softDelete(request.user, version)
    }
    NoContent
  }


  private def getVersion(user: User, org: String, service: String, version: String): Option[Version] = {
    OrganizationDao.findByUserAndKey(user, org).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, service).flatMap { service =>
        if (version == "latest") {
          VersionDao.findAll(service_guid = service.guid, limit = 1).headOption
        } else {
          VersionDao.findByServiceAndVersion(service, version)
        }
      }
    }
  }

}
