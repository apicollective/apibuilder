package controllers

import core.{ ServiceDescription, ServiceDescriptionValidator }
import core.{ Organization, User }
import db.{ Service, Version, OrganizationDao, VersionDao, VersionQuery }
import play.api.mvc._
import play.api.libs.json.Json

object Versions extends Controller {

  def getServiceVersion(org: String, service: String, version: String) = Authenticated { request =>
    getVersion(request.user, org, service, version) match {
      case None => NotFound
      case Some(v: Version) => Ok(Json.toJson(v))
    }
  }

  def putServiceVersion(orgKey: String, serviceKey: String, version: String) = Authenticated(parse.json) { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => BadRequest(s"Organization[$orgKey] does not exist or you are not authorized to access it")
      case Some(org: Organization) => {

        val serviceDescription = ServiceDescription(request.body)
        val errors = ServiceDescriptionValidator(serviceDescription).validate

        if (errors.isEmpty) {
          val service = Service.findByOrganizationAndKey(org, serviceKey).getOrElse {
            Service.create(request.user, org, serviceDescription.name, Some(serviceKey))
          }

          VersionDao.findByServiceAndVersion(service, version) match {
            case None => VersionDao.create(request.user, service, version, request.body.toString)
            case Some(existing: Version) => existing.replace(request.user, service, request.body.toString)
          }

          NoContent

        } else {
          BadRequest(errors.mkString(", "))

        }
      }
    }
  }

  def deleteServiceVersion(org: String, service: String, version: String) = Authenticated { request =>
    getVersion(request.user, org, service, version).map { version =>
      version.softDelete(request.user)
    }
    NoContent
  }


  private def getVersion(user: User, org: String, service: String, version: String): Option[Version] = {
    OrganizationDao.findByUserAndKey(user, org).flatMap { org =>
      Service.findByOrganizationAndKey(org, service).flatMap { service =>
        VersionDao.findByServiceAndVersion(service, version)
      }
    }
  }

}
