package controllers

import db.OrganizationDomainsDao
import io.apibuilder.api.v0.models.Domain
import io.apibuilder.api.v0.models.json._
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}

@Singleton
class Domains @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  organizationDomainsDao: OrganizationDomainsDao
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

  def deleteByName(orgKey: String, name: String): Action[AnyContent] = Identified { request =>
    withOrgAdmin(request.user, orgKey) { org =>
      organizationDomainsDao.findAll(
        organizationGuid = Some(org.guid),
        domain = Some(name)
      ).foreach { domain =>
        organizationDomainsDao.softDelete(request.user, domain)
      }
      NoContent
    }
  }

}
