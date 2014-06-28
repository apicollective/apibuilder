package controllers

import lib.Validation
import db.{ Organization, OrganizationDao }
import play.api.mvc._
import play.api.libs.json.Json

object Organizations extends Controller {

  def get(guid: Option[String], userGuid: Option[String], key: Option[String], name: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val orgs = OrganizationDao.findAll(userGuid = userGuid,
                                       guid = guid,
                                       key = key,
                                       name = name,
                                       limit = limit,
                                       offset = offset)
    Ok(Json.toJson(orgs))
  }

  def post() = Authenticated(parse.json) { request =>
    (request.body \ "name").asOpt[String] match {
      case None => {
        Conflict(Json.toJson(Validation.error("name is required")))
      }

      case Some(name: String) => {
        OrganizationDao.findAll(name = Some(name), limit = 1).headOption match {
          case None => {
            val org = OrganizationDao.createWithAdministrator(request.user, name)
            Ok(Json.toJson(org))
          }

          case Some(org: Organization) => {
            Conflict(Json.toJson(Validation.error("Org with this name already exists")))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: String) = Authenticated { request =>
    OrganizationDao.findByGuid(guid).map { organization =>
      OrganizationDao.softDelete(request.user, organization)
    }
    NoContent
  }

}
