package controllers

import java.util.UUID

import com.gilt.apidoc.models.json._
import db.{GeneratorDao, GeneratorForm, OrganizationDao}
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

object Generators extends Controller {

  def getByGuid(orgKey: String, guid: UUID) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) =>
        request.requireAdmin(org)
        GeneratorDao.findAll(orgGuid = org.guid, guid = Some(guid)).headOption match {
          case None => NotFound
          case Some(g) => Ok(Json.toJson(g))

        }
    }
  }

  def get(orgKey: String) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) => Ok(Json.toJson(GeneratorDao.findAll(orgGuid = org.guid)))
    }
  }

  def post(orgKey: String) = Authenticated(parse.json) { request =>
    request.body.validate[GeneratorForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[GeneratorForm] => {
        val form = s.get
        OrganizationDao.findByUserAndKey(request.user, orgKey) match {
          case None => NotFound
          case Some(org) =>
            request.requireAdmin(org)
            GeneratorDao.findAll(orgGuid = org.guid, uri = Some(form.uri)).headOption match {
              case None =>
                val generator = GeneratorDao.create(request.user, org, form.name, form.uri)
                Ok(Json.toJson(generator))
              case Some(d) =>
                Conflict(Json.toJson(Validation.error("generator uri already exists for this org")))
            }
        }
      }
    }
  }

  def deleteByGuid(orgKey: String, guid: UUID) = Authenticated { request =>
    OrganizationDao.findByUserAndKey(request.user, orgKey) match {
      case None => NotFound
      case Some(org) =>
        request.requireAdmin(org)
        GeneratorDao.findAll(orgGuid = org.guid, guid = Some(guid)).headOption match {
          case None => NotFound
          case Some(g) =>
            GeneratorDao.softDelete(request.user, g)
        }
    }
    NoContent
  }

}
