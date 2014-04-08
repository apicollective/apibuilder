package controllers

import db.{ Organization, OrganizationDao }
import play.api.mvc._
import play.api.libs.json.Json

object Organizations extends Controller {

  def get(guid: Option[String], key: Option[String], name: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val orgs = OrganizationDao.findAll(user_guid = request.user.guid,
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
        BadRequest("name is required")
      }

      case Some(name: String) => {
        OrganizationDao.findByUserAndName(request.user, name) match {
          case None => {
            val org = OrganizationDao.createWithAdministrator(request.user, name)
            Ok(Json.toJson(org))
          }

          case Some(org: Organization) => {
            BadRequest("Org with this name already exists")
          }
        }
      }
    }
  }

}
