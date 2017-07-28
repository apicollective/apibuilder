package controllers

import io.apibuilder.api.v0.models.json._
import io.apibuilder.api.v0.models.Domain
import db.{OrganizationsDao, OrganizationDomainsDao}
import javax.inject.{Inject, Singleton}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Domains @Inject() (
  organizationsDao: OrganizationsDao,
  organizationDomainsDao: OrganizationDomainsDao
) extends Controller {

  def post(orgKey: String) = Authenticated(parse.json) { request =>
    request.body.validate[Domain] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[Domain] => {
        val form = s.get
        organizationsDao.findByUserAndKey(request.user, orgKey) match {
          case None => NotFound
          case Some(org) => {
            request.requireAdmin(org)
            organizationDomainsDao.findAll(domain = Some(form.name)).headOption match {
              case None => {
                val od = organizationDomainsDao.create(request.user, org, form.name)
                Ok(Json.toJson(od.domain))
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
    organizationsDao.findByUserAndKey(request.user, orgKey).map { org =>
      request.requireAdmin(org)
      org.domains.find(_.name == name).map { domain =>
        organizationDomainsDao.findAll(organizationGuid = Some(org.guid), domain = Some(domain.name)).map { orgDomain =>
          organizationDomainsDao.softDelete(request.user, orgDomain)
        }
      }
    }
    NoContent
  }

}
