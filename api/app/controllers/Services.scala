package controllers

import com.gilt.apidoc.models.json._
import db.{Authorization, OrganizationDao, ServiceDao, ServiceForm}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object Services extends Controller {

  def getByOrgKey(orgKey: String, name: Option[String], key: Option[String], limit: Int = 25, offset: Int = 0) = ApiRequest { request =>
    val services = ServiceDao.findAll(
      Authorization(request.user),
      orgKey = orgKey,
      name = name,
      key = key,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(services))
  }

  def putByOrgKeyAndServiceKey(orgKey: String, serviceKey: String) = Authenticated(parse.json) { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => {
        request.requireAdmin(org)

        request.body.validate[ServiceForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
          }
          case s: JsSuccess[ServiceForm] => {
            val form = s.get
            ServiceDao.findByOrganizationKeyAndServiceKey(Authorization.User(request.user.guid), org.key, serviceKey) match {
              case None => Conflict(Json.toJson(Validation.error(s"service[$serviceKey] not found or inaccessible")))
              case Some(existing) => {
                val errors = ServiceDao.validate(org, form, Some(existing))
                if (errors.isEmpty) {
                  val service = existing.copy(
                    name = form.name,
                    description = form.description,
                    visibility = form.visibility
                  )
                  ServiceDao.update(request.user, service)
                  NoContent
                } else {
                  Conflict(Json.toJson(errors))
                }
              }
            }
          }
        }
      }
    }
  }

  def deleteByOrgKeyAndServiceKey(orgKey: String, serviceKey: String) = Authenticated { request =>
    OrganizationDao.findByKey(Authorization.User(request.user.guid), orgKey) map { org =>
      request.requireAdmin(org)
      ServiceDao.findByOrganizationKeyAndServiceKey(Authorization.User(request.user.guid), orgKey, serviceKey).map { service =>
        ServiceDao.softDelete(request.user, service)
      }
    }
    NoContent
  }

}
