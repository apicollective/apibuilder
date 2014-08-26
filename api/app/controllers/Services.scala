package controllers

import com.gilt.apidoc.models.json._
import db.{ OrganizationDao, ServiceDao }
import play.api.mvc._
import play.api.libs.json.Json

object Services extends Controller {

  def getByOrgKey(orgKey: String, name: Option[String], key: Option[String], limit: Int = 25, offset: Int = 0) = Authenticated { request =>
    val services = ServiceDao.findAll(orgKey = orgKey,
                                      name = name,
                                      key = key,
                                      limit = limit,
                                      offset = offset)
    Ok(Json.toJson(services))
  }

  def deleteByOrgKeyAndServiceKey(orgKey: String, serviceKey: String) = Authenticated { request =>
    OrganizationDao.findAll(userGuid = Some(request.user.guid), key = Some(orgKey), limit = 1).headOption.map { org =>
      ServiceDao.findByOrganizationAndKey(org, serviceKey).map { service =>
        ServiceDao.softDelete(request.user, service)
      }
    }

    NoContent
  }

}
