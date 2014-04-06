package controllers

import db.{ OrganizationDao, Service, ServiceQuery }
import play.api.mvc._
import play.api.libs.json.Json

object Services extends Controller {

  def get(org: String, name: Option[String], key: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val services = Service.findAll(ServiceQuery(orgKey = org,
                                                name = name,
                                                key = key,
                                                limit = limit,
                                                offset = offset))
    Ok(Json.toJson(services))
  }

  def deleteService(org: String, service: String) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, org).map { org =>
      Service.findByOrganizationAndKey(org, service).map { service =>
        service.softDelete(request.user)
      }
    }

    NoContent
  }

}
