package controllers

import db.{ Domain, OrganizationDao, OrganizationDomainDao }
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object Domains extends Controller {

  def post(orgKey: String) = Authenticated(parse.json) { request =>
    request.body.validate[Domain] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[Domain] => {
        val form = s.get
        OrganizationDao.findByUserAndKey(request.user, orgKey) match {
          case None => NotFound
          case Some(org) => {
            println("DOMAIN TO ADD: " + form)
            Ok(Json.toJson(form))
          }
        }
      }
    }
  }

  def deleteByName(orgKey: String, name: String) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey).map { org =>
      org.domains.find(_.name == name).map { domain =>
        OrganizationDomainDao.findAll(organizationGuid = Some(org.guid), domain = Some(domain.name)).map { orgDomain =>
          OrganizationDomainDao.softDelete(request.user, orgDomain)
        }
      }
    }
    NoContent
  }

}
