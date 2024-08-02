package controllers

import db.InternalOrganizationDomainsDao
import io.apibuilder.api.v0.models.Domain
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import models.DomainsModel
import play.api.libs.json.*
import play.api.mvc.*

import javax.inject.{Inject, Singleton}

@Singleton
class Domains @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  organizationDomainsDao: InternalOrganizationDomainsDao,
  domainsModel: DomainsModel
) extends ApiBuilderController {

  def post(orgKey: String): Action[JsValue] = Identified(parse.json) { request =>
    withOrgAdmin(request.user, orgKey) { org =>
      request.body.validate[Domain] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[Domain] => {
          val form = s.get
          organizationDomainsDao.findAll(
            organizationGuid = Some(org.guid),
            domain = Some(form.name),
            limit = Some(1)
          ).headOption match {
            case None => {
              val od = organizationDomainsDao.create(request.user, org, form.name)
              Ok(Json.toJson(domainsModel.toModel(od)))
            }
            case Some(_) => {
              Conflict(Json.toJson(Validation.error("domain has already been registered")))
            }
          }
        }
      }
    }
  }

  def deleteByName(orgKey: String, name: String): Action[AnyContent] = Identified { request =>
    withOrgAdmin(request.user, orgKey) { org =>
      organizationDomainsDao.findAll(
        organizationGuid = Some(org.guid),
        domain = Some(name),
        limit = Some(1)
      ).foreach { domain =>
        organizationDomainsDao.softDelete(request.user, domain)
      }
      NoContent
    }
  }

}
