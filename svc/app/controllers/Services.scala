package controllers

import db.{ OrganizationDao, ServiceDao }
import play.api.mvc._
import play.api.libs.json.Json

object Services extends Controller {

  def get(org: String, name: Option[String], key: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val services = ServiceDao.findAll(org_key = org,
                                      name = name,
                                      key = key,
                                      limit = limit,
                                      offset = offset)
    Ok(Json.toJson(services))
  }

  def deleteService(org: String, service: String) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, org).map { org =>
      ServiceDao.findByOrganizationAndKey(org, service).map { service =>
        ServiceDao.softDelete(request.user, service)
      }
    }

    NoContent
  }

}
