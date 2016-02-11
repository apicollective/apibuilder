package controllers

import db.AttributesDao
import lib.Validation
import com.bryzek.apidoc.api.v0.models.{User, Attribute, AttributeForm}
import com.bryzek.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Attributes extends Controller with Attributes

trait Attributes {
  this: Controller =>

  def get(
    guid: Option[UUID],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val attributes = AttributesDao.findAll(
      guid = guid,
      name = name,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(attributes))
  }

  def getByGuid(guid: UUID) = Authenticated { request =>
    AttributesDao.findByGuid(guid) match {
      case None => NotFound
      case Some(attribute) => Ok(Json.toJson(attribute))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[AttributeForm] match {
      case e: JsError => {
        BadRequest(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[AttributeForm] => {
        val form = s.get
        AttributesDao.validate(form) match {
          case Nil => {
            val attribute = AttributesDao.create(request.user, form)
            Created(Json.toJson(attribute))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    AttributesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(attribute) => {
        attribute.audit.createdBy.guid == request.user.guid match {
          case false => {
            Unauthorized
          }
          case true => {
            AttributesDao.softDelete(request.user, attribute)
            NoContent
          }
        }
      }
    }
  }

}
