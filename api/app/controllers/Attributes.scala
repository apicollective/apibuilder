package controllers

import cats.data.Validated.{Invalid, Valid}
import db.InternalAttributesDao
import lib.Validation
import io.apibuilder.api.v0.models.AttributeForm
import io.apibuilder.api.v0.models.json.*
import models.AttributesModel
import play.api.mvc.*
import play.api.libs.json.*

import javax.inject.{Inject, Singleton}
import java.util.UUID

@Singleton
class Attributes @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  attributesDao: InternalAttributesDao,
  model: AttributesModel
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
      limit = Some(limit),
      offset = offset
    )
    Ok(Json.toJson(model.toModels(attributes)))
  }

  def getByName(name: String): Action[AnyContent] = Action { _ =>
    attributesDao.findByName(name) match {
      case None => NotFound
      case Some(attribute) => Ok(Json.toJson(model.toModel(attribute)))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[AttributeForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: AttributeForm, _)=> {
        attributesDao.create(request.user, form) match {
          case Valid(attribute) => Created(Json.toJson(model.toModel(attribute)))
          case Invalid(errors) => Conflict(Json.toJson(errors.toNonEmptyList.toList))
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
        if (attribute.db.createdByGuid == request.user.guid) {
          attributesDao.softDelete(request.user, attribute)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
