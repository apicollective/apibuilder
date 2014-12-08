package controllers

import com.gilt.apidoc.models.OrganizationMetadataForm
import com.gilt.apidoc.models.json._
import db.{ OrganizationDao, OrganizationMetadataDao }
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object OrganizationMetadata extends Controller {

  def put(key: String) = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationMetadataForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationMetadataForm] => {
        val form = s.get
        OrganizationDao.findByUserAndKey(request.user, key) match {
          case None => NotFound
          case Some(org) => {
            request.requireAdmin(org)
            OrganizationMetadataDao.validate(form) match {
              case Nil => {
                val metadata = OrganizationMetadataDao.upsert(request.user, org, form)
                Ok(Json.toJson(metadata))
              }
              case errors => {
                Conflict(Json.toJson(errors))
              }
            }
          }
        }
      }
    }
  }

}
