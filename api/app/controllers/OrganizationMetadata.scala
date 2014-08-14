package controllers

import db.{ OrganizationDao, OrganizationMetadataDao, OrganizationMetadataForm }
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

object OrganizationMetadata extends Controller {

  def put(key: String) = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationMetadataForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[OrganizationMetadataForm] => {
        val form = s.get.copy(package_name = s.get.package_name.map(_.trim))
        OrganizationDao.findByUserAndKey(request.user, key) match {
          case None => NotFound
          case Some(org) => {
            OrganizationMetadataForm.validate(form) match {
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
