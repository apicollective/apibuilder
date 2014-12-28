package controllers

import com.gilt.apidoc.models.json._
import com.gilt.apidoc.models.Domain
import db.{OrganizationsDao, OrganizationDomainsDao}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object Domains extends Controller {

  def post(orgKey: String) = Authenticated(parse.json) { request =>
    request.body.validate[Domain] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[Domain] => {
        val form = s.get
        OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
          case None => NotFound
          case Some(org) => {
            request.requireAdmin(org)
            OrganizationDomainsDao.findAll(domain = Some(form.name)).headOption match {
              case None => {
                val od = OrganizationDomainsDao.create(request.user, org, form.name)
                Ok(Json.toJson(od.toDomain))
              }
              case Some(d) => {
                Conflict(Json.toJson(Validation.error("domain has already been registered")))
              }
            }
          }
        }
      }
    }
  }

  def deleteByName(orgKey: String, name: String) = Authenticated { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey).map { org =>
      request.requireAdmin(org)
      org.domains.find(_.name == name).map { domain =>
        OrganizationDomainsDao.findAll(organizationGuid = Some(org.guid), domain = Some(domain.name)).map { orgDomain =>
          OrganizationDomainsDao.softDelete(request.user, orgDomain)
        }
      }
    }
    NoContent
  }

}
