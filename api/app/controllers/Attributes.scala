package controllers

import db.AttributesDao
import lib.Validation
import io.apibuilder.api.v0.models.AttributeForm
import io.apibuilder.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import java.util.UUID

@Singleton
class Attributes @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  attributesDao: AttributesDao
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { _ =>
    val attributes = attributesDao.findAll(
      guid = guid,
      name = name,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(attributes))
  }

  def getByName(name: String): Action[AnyContent] = Action { _ =>
    attributesDao.findByName(name) match {
      case None => NotFound
      case Some(attribute) => Ok(Json.toJson(attribute))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[AttributeForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[AttributeForm] => {
        val form = s.get
        attributesDao.validate(form) match {
          case Nil => {
            val attribute = attributesDao.create(request.user, form)
            Created(Json.toJson(attribute))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByName(name: String): Action[AnyContent] = Identified { request =>
    attributesDao.findByName(name) match {
      case None => {
        NotFound
      }
      case Some(attribute) => {
        if (attribute.audit.createdBy.guid == request.user.guid) {
          attributesDao.softDelete(request.user, attribute)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
