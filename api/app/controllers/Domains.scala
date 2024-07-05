package controllers

import io.apibuilder.api.v0.models.json._
import io.apibuilder.api.v0.models.Domain
import db.{MembershipsDao, OrganizationDomainsDao, OrganizationsDao}
import javax.inject.{Inject, Singleton}

import lib.Validation
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Domains @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  organizationDomainsDao: OrganizationDomainsDao
) extends ApiBuilderController {

  def post(orgKey: String) = Identified.async(parse.json) { request =>
    withOrgAdmin(request.user, orgKey) { org =>
      request.body.validate[Domain] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[Domain] => {
          val form = s.get
          organizationDomainsDao.findAll(
            organizationGuid = Some(org.guid),
            domain = Some(form.name)
          ).headOption match {
            case None => {
              val od = organizationDomainsDao.create(request.user, org, form.name)
              Ok(Json.toJson(od.domain))
            }
            case Some(_) => {
              Conflict(Json.toJson(Validation.error("domain has already been registered")))
            }
          }
        }
      }
    }
  }

  def deleteByName(orgKey: String, name: String) = Identified.async { request =>
    withOrgAdmin(request.user, orgKey) { org =>
      org.domains.find(_.name == name).foreach { domain =>
        organizationDomainsDao.findAll(organizationGuid = Some(org.guid), domain = Some(domain.name)).foreach { orgDomain =>
          organizationDomainsDao.softDelete(request.user, orgDomain)
        }
      }
      NoContent
    }
  }

}
