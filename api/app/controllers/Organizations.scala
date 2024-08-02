package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.*
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import db.*
import models.{OrganizationAttributeValuesModel, OrganizationsModel}

import javax.inject.{Inject, Singleton}
import play.api.mvc.*
import play.api.libs.json.*

import java.util.UUID

@Singleton
class Organizations @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  attributesDao: InternalAttributesDao,
  organizationAttributeValuesDao: InternalOrganizationAttributeValuesDao,
  organizationAttributeValuesModel: OrganizationAttributeValuesModel,
  model: OrganizationsModel
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    userGuid: Option[UUID],
    key: Option[String],
    name: Option[String],
    namespace: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { request =>
    Ok(
      Json.toJson(
        model.toModels(
          organizationsDao.findAll(
            request.authorization,
            userGuid = userGuid,
            guid = guid,
            key = key,
            name = name,
            namespace = namespace,
            limit = Some(limit),
            offset = offset
          )
        )
      )
    )
  }

  def getByKey(key: String): Action[AnyContent] = Anonymous { request =>
    withOrg(request.authorization, key) { org =>
      Ok(Json.toJson(model.toModel(org)))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationForm] => {
        val form = s.get
        val errors = organizationsDao.validate(form)
        if (errors.isEmpty) {
          val org = organizationsDao.createWithAdministrator(request.user, form)
          Ok(Json.toJson(model.toModel(org)))
        } else {
          Conflict(Json.toJson(errors))
        }
      }
    }
  }

  def putByKey(key: String): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: OrganizationForm, _) => { 
        organizationsDao.findByKey(request.authorization, key) match {
          case None => NotFound
          case Some(existing) => {
            val errors = organizationsDao.validate(form, Some(existing))
            if (errors.isEmpty) {
              val org = organizationsDao.update(request.user, existing, form)
              Ok(Json.toJson(model.toModel(org)))
            } else {
              Conflict(Json.toJson(errors))
            }
          }
        }
      }
    }
  }

  def deleteByKey(key: String): Action[AnyContent] = Identified { request =>
    withOrgAdmin(request.user, key) { org =>
      organizationsDao.softDelete(request.user, org)
      NoContent
    }
  }

  def getAttributesByKey(
    key: String,
    attributeName: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    withOrg(request.authorization, key) { org =>
      Ok(
        Json.toJson(
          organizationAttributeValuesModel.toModels(
            organizationAttributeValuesDao.findAll(
              organizationGuid = Some(org.guid),
              attributeName = attributeName,
              limit = Some(limit),
              offset = offset
            )
          )
        )
      )
    }
  }

  def getAttributesByKeyAndName(
    key: String,
    name: String
  ): Action[AnyContent] = Identified { request =>
    withOrg(request.authorization, key) { org =>
      organizationAttributeValuesDao.findByOrganizationGuidAndAttributeName(org.guid, name) match {
        case None => NotFound
        case Some(attr) => Ok(Json.toJson(organizationAttributeValuesModel.toModel(attr)))
      }
    }
  }

  def putAttributesByKeyAndName(key: String, name: String): Action[JsValue] = Identified(parse.json) { request =>
    withOrg(request.authorization, key) { org =>
      withAttribute(name) { attr =>
        request.body.validate[AttributeValueForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case JsSuccess(form: AttributeValueForm, _) => {
            val existing = organizationAttributeValuesDao.findByOrganizationGuidAndAttributeName(org.guid, name)
            organizationAttributeValuesDao.upsert(request.user, org, attr, form)
              .map(organizationAttributeValuesModel.toModel) match {
              case Valid(value) => {
                existing match {
                  case None => Created(Json.toJson(value))
                  case Some(_) => Ok(Json.toJson(value))
                }
              }
              case Invalid(errors) => {
                Conflict(Json.toJson(errors.toNonEmptyList.toList))
              }
            }
          }
        }
      }
    }
  }

  def deleteAttributesByKeyAndName(
    key: String,
    name: String
  ): Action[AnyContent] = Identified { request =>
    withOrg(request.authorization, key) { org =>
      organizationAttributeValuesDao.findByOrganizationGuidAndAttributeName(org.guid, name) match {
        case None => NotFound
        case Some(attr) => {
          organizationAttributeValuesDao.softDelete(request.user, attr)
          NoContent
        }
      }
    }
  }

  private def withAttribute(
    name: String
  ) (
    f: InternalAttribute => Result
  ) = {
    attributesDao.findByName(name) match {
      case None => {
        NotFound
      }
      case Some(attr) => {
        f(attr)
      }
    }
  }

}
