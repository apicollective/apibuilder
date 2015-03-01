package controllers

import com.gilt.apidoc.v0.models.{Application, ApplicationForm, Organization, Original, OriginalType, User, Version, VersionForm, Visibility}
import com.gilt.apidoc.v0.models.json._
import com.gilt.apidoc.spec.v0.models.Service
import lib.Validation
import core.{ServiceConfiguration, ServiceValidator}
import db.{ApplicationsDao, Authorization, OrganizationsDao, VersionValidator, VersionsDao}
import play.api.mvc._
import play.api.libs.json._

object Versions extends Controller {

  private val DefaultVisibility = Visibility.Organization

  def getByOrgKeyAndApplicationKey(orgKey: String, applicationKey: String, limit: Long = 25, offset: Long = 0) = AnonymousRequest { request =>
    val versions = ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization(request.user), orgKey, applicationKey).map { application =>
      VersionsDao.findAll(
        Authorization(request.user),
        applicationGuid = Some(application.guid),
        limit = limit,
        offset = offset
      )
    }.getOrElse(Seq.empty)
    Ok(Json.toJson(versions))
  }

  def getByOrgKeyAndApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = AnonymousRequest { request =>
    VersionsDao.findVersion(Authorization(request.user), orgKey, applicationKey, version) match {
      case None => NotFound
      case Some(v: Version) => Ok(Json.toJson(v))
    }
  }

  def postByOrgKeyAndVersion(
    orgKey: String,
    versionName: String
  ) = Authenticated(parse.json) { request =>
    val original = Original(
      `type` = OriginalType.ApiJson,
      data = request.body.toString
    )

    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => {
        Conflict(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }
      case Some(org) => {
        request.body.validate[VersionForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[VersionForm] => {
            val form = s.get
            val validator = ServiceValidator(ServiceConfiguration(org, versionName), form.serviceForm.toString)
            val errors = validator.errors ++ validate(request.user, org, validator)

            errors match {
              case Nil => {
                val version = upsertVersion(request.user, org, versionName, form, original, validator.service.get)
                Ok(Json.toJson(version))
              }
              case errors => {
                Conflict(Json.toJson(Validation.errors(errors)))
              }
            }
          }
        }
      }
    }
  }

  def putByOrgKeyAndApplicationKeyAndVersion(
    orgKey: String,
    applicationKey: String,
    versionName: String
  ) = Authenticated(parse.json) { request =>
    val original = Original(
      `type` = OriginalType.ApiJson,
      data = request.body.toString
    )

    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => {
        Conflict(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }

      case Some(org: Organization) => {

        request.body.validate[VersionForm] match {
          case e: JsError => {
            Conflict(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[VersionForm] => {
            val form = s.get
            val validator = ServiceValidator(ServiceConfiguration(org, versionName), form.serviceForm.toString)
            val errors = validator.errors ++ validate(request.user, org, validator, Some(applicationKey))

            errors match {
              case Nil => {
                val version = upsertVersion(request.user, org, versionName, form, original, validator.service.get, Some(applicationKey))
                Ok(Json.toJson(version))
              }
              case errors => {
                Conflict(Json.toJson(Validation.errors(errors)))
              }
            }
          }
        }
      }
    }
  }

  def deleteByOrgKeyAndApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = Authenticated { request =>
    val auth = Authorization.User(request.user.guid)
    OrganizationsDao.findByKey(auth, orgKey) map { org =>
      request.requireAdmin(org)
      VersionsDao.findVersion(auth, orgKey, applicationKey, version).map { version =>
        VersionsDao.softDelete(request.user, version)
      }
    }
    NoContent
  }

  private def upsertVersion(
    user: User,
    org: Organization,
    versionName: String,
    form: VersionForm,
    original: Original,
    service: Service,
    applicationKey: Option[String] = None
  ): Version = {
    val application = applicationKey.flatMap { key => ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, key) } match {
      case None => {
        val appForm = ApplicationForm(
          name = service.name,
          description = service.description,
          visibility = form.visibility.getOrElse(DefaultVisibility)
        )
        ApplicationsDao.create(user, org, appForm, applicationKey)
      }
      case Some(app) => {
        form.visibility.map { v =>
          if (app.visibility != v) {
            ApplicationsDao.setVisibility(user, app, v)
          }
        }
        app
      }
    }

    VersionsDao.findByApplicationAndVersion(Authorization(Some(user)), application, versionName) match {
      case None => VersionsDao.create(user, application, versionName, original, service)
      case Some(existing: Version) => VersionsDao.replace(user, existing, application, original, service)
    }
  }

  private def validate(
    user: User,
    org: Organization,
    validator: ServiceValidator,
    existingKey: Option[String] = None
  ): Seq[String] = {
    validator.service match {
      case None => Seq.empty
      case Some(service) => {
        VersionValidator(
          user = user,
          org = org,
          newApplicationKey = service.application.key,
          existingApplicationKey = existingKey
        ).validate
      }
    }
  }

}
