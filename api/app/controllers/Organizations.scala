package controllers

import com.gilt.apidoc.models.{Organization, OrganizationForm}
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{Authorization, OrganizationDao}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Organizations extends Controller {

  def get(
    guid: Option[UUID],
    userGuid: Option[UUID],
    key: Option[String],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    Ok(
      Json.toJson(
        OrganizationDao.findAll(
          Authorization(request.user),
          userGuid = userGuid,
          guid = guid,
          key = key,
          name = name,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByKey(key: String) = AnonymousRequest { request =>
    OrganizationDao.findByKey(Authorization(request.user), key) match {
      case None => NotFound
      case Some(org) => Ok(Json.toJson(org))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
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
      request.requireAdmin(organization)
      OrganizationDao.softDelete(request.user, organization)
    }
    NoContent
  }

}
