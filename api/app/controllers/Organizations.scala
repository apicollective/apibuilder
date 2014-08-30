package controllers

import com.gilt.apidoc.models.Organization
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{Authorization, OrganizationDao, OrganizationForm}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Organizations extends Controller {

  // TODO: Remove userGuid
  def get(guid: Option[UUID], userGuid: Option[UUID], key: Option[String], name: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    println(s"Authorization.User(${request.user.guid})")

    val orgs = OrganizationDao.findAll(Authorization.User(request.user.guid),
                                       guid = guid,
                                       key = key,
                                       name = name,
                                       limit = limit,
                                       offset = offset)
    Ok(Json.toJson(orgs))
  }

  def getByKey(key: String) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, key) match {
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
    OrganizationDao.findByUserAndKey(request.user, key).map { organization =>
      OrganizationDao.softDelete(request.user, organization)
    }
    NoContent
  }

}
