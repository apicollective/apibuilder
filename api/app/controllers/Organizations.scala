package controllers

import com.bryzek.apidoc.api.v0.models.{AttributeValueForm, Organization, OrganizationForm, User}
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.{Authorization, OrganizationsDao, OrganizationAttributeValuesDao}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Organizations extends Controller {

  def get(
    guid: Option[UUID],
    userGuid: Option[UUID],
    key: Option[String],
    name: Option[String],
    namespace: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    Ok(
      Json.toJson(
        OrganizationsDao.findAll(
          request.authorization,
          userGuid = userGuid,
          guid = guid,
          key = key,
          name = name,
          namespace = namespace,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByKey(key: String) = AnonymousRequest { request =>
    OrganizationsDao.findByKey(request.authorization, key) match {
      case None => NotFound
      case Some(org) => Ok(Json.toJson(org))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationForm] => {
        val form = s.get
        val errors = OrganizationsDao.validate(form)
        if (errors.isEmpty) {
          val org = OrganizationsDao.createWithAdministrator(request.user, form)
          Ok(Json.toJson(org))
        } else {
          Conflict(Json.toJson(errors))
        }
      }
    }
  }

  def putByKey(key: String) = Authenticated(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationForm] => { 
        OrganizationsDao.findByKey(request.authorization, key) match {
          case None => NotFound
          case Some(existing) => {
            val form = s.get
            val errors = OrganizationsDao.validate(form, Some(existing))
            if (errors.isEmpty) {
              val org = OrganizationsDao.update(request.user, existing, form)
              Ok(Json.toJson(org))
            } else {
              Conflict(Json.toJson(errors))
            }
          }
        }
      }
    }
  }

  def deleteByKey(key: String) = Authenticated { request =>
    OrganizationsDao.findByUserAndKey(request.user, key).map { organization =>
      request.requireAdmin(organization)
      OrganizationsDao.softDelete(request.user, organization)
    }
    NoContent
  }

  def getAttributesByKey(
    key: String,
    attributeName: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    withOrganization(request.user, key) { org =>
      Ok(
        Json.toJson(
          OrganizationAttributeValuesDao.findAll(
            organizationGuid = Some(org.guid),
            attributeName = attributeName,
            limit = limit,
            offset = offset
          )
        )
      )
    }
  }

  def getAttributesByKeyAndName(
    key: String,
    name: String
  ) = Authenticated { request =>
    withOrganization(request.user, key) { org =>
      OrganizationAttributeValuesDao.findByOrganizationGuidAndAttributeName(org.guid, name) match {
        case None => NotFound
        case Some(attr) => Ok(Json.toJson(attr))
      }
    }
  }

  def postAttributesByKey(key: String) = Authenticated(parse.json) { request =>
    withOrganization(request.user, key) { org =>
      request.body.validate[AttributeValueForm] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[AttributeValueForm] => {
          val form = s.get
          OrganizationAttributeValuesDao.validate(org, form) match {
            case Nil => {
              val attr = OrganizationAttributeValuesDao.create(request.user, org, form)
              Created(Json.toJson(attr))
            }
            case errors => {
              Conflict(Json.toJson(errors))
            }
          }
        }
      }
    }
  }

  def deleteAttributesByKeyAndName(
    key: String,
    name: String
  ) = Authenticated { request =>
    withOrganization(request.user, key) { org =>
      OrganizationAttributeValuesDao.findByOrganizationGuidAndAttributeName(org.guid, name) match {
        case None => NotFound
        case Some(attr) => {
          OrganizationAttributeValuesDao.softDelete(request.user, attr)
          NoContent
        }
      }
    }
  }

  private[this] def withOrganization(
    user: User,
    key: String
  ) (
    f: Organization => Result
  ) = {
    OrganizationsDao.findByKey(Authorization.User(user.guid), key) match {
      case None => {
        NotFound
      }
      case Some(org) => {
        f(org)
      }
    }
  }

}
