package controllers

import lib.Validation
import db.{ Organization, OrganizationDao }
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object Organizations extends Controller {

  def get(guid: Option[UUID], userGuid: Option[UUID], key: Option[String], name: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val orgs = OrganizationDao.findAll(userGuid = userGuid.map(_.toString),
                                       guid = guid.map(_.toString),
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
        val errors = OrganizationDao.validate(name)
        if (errors.isEmpty) {
          val org = OrganizationDao.createWithAdministrator(request.user, name)
          Ok(Json.toJson(org))
        } else {
          Conflict(Json.toJson(errors))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    OrganizationDao.findByGuid(guid).map { organization =>
      OrganizationDao.softDelete(request.user, organization)
    }
    NoContent
  }

}
