package controllers

import lib.Validation
import db.{ Organization, OrganizationDao, OrganizationForm }
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Organizations extends Controller {

  def get(guid: Option[UUID], userGuid: Option[UUID], key: Option[String], name: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val orgs = OrganizationDao.findAll(userGuid = userGuid,
                                       guid = guid.map(_.toString),
                                       key = key,
                                       name = name,
                                       limit = limit,
                                       offset = offset)
    Ok(Json.toJson(orgs))
  }

  def getByKey(key: String) = Authenticated { request =>
    OrganizationDao.findAll(key = Some(key), limit = 1).headOption match {
      case None => NotFound
      case Some(org) => Ok(Json.toJson(org))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[OrganizationForm] => {
        val form = s.get
        val errors = OrganizationDao.validate(form)
        if (errors.isEmpty) {
          val org = OrganizationDao.createWithAdministrator(request.user, form)
          Ok(Json.toJson(org))
        } else {
          Conflict(Json.toJson(errors))
        }
      }
    }
  }

  def deleteByKey(key: String) = Authenticated { request =>
    OrganizationDao.findAll(key = Some(key), limit = 1).headOption.map { organization =>
      OrganizationDao.softDelete(request.user, organization)
    }
    NoContent
  }

}
