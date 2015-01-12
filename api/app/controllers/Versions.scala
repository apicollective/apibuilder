package controllers

import com.gilt.apidoc.models.{Application, ApplicationForm, Organization, User, Version, VersionForm, Visibility}
import com.gilt.apidoc.models.json._
import com.gilt.apidocspec.models.Service
import lib.Validation
import core.{ServiceConfiguration, ServiceValidator}
import db.{ApplicationsDao, Authorization, OrganizationsDao, VersionsDao}
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
            val errors = validator.errors ++ VersionFormValidator(request.user, validator, org).validate

            errors match {
              case Nil => {
                val version = upsertVersion(request.user, org, versionName, form, validator.serviceForm.get, validator.service.get)
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

            val errors = validator.errors ++ VersionFormValidator(request.user, validator, org, Some(applicationKey)).validate

            errors match {
              case Nil => {
                val version = upsertVersion(request.user, org, versionName, form, validator.serviceForm.get, validator.service.get, Some(applicationKey))
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
    serviceForm: JsObject, // TODO: Change to ServiceForm once ready
    service: Service,
    applicationKey: Option[String] = None
  ): Version = {
    println("applicationKey: " + applicationKey)
    println("service.key: " + service.application.key)

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
      case None => VersionsDao.create(user, application, versionName, serviceForm, service)
      case Some(existing: Version) => VersionsDao.replace(user, existing, application, serviceForm, service)
    }
  }

}


case class VersionFormValidator(
  user: User,
  validator: ServiceValidator,
  org: Organization,
  key: Option[String] = None
) {

  val validate: Seq[String] = {
    existing match {
      case None => validateAppDoesNotExist()
      case Some(app) => validateCanUpdate(app) ++ validateKey()
    }
  }

  private lazy val existing = key.flatMap { ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, _) }

  private def validateAppDoesNotExist(): Seq[String] = {
    validator.service match {
      case None => Seq.empty
      case Some(service) => {
        ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, service.application.key) match {
          case None => Seq.empty
          case Some(app) => Seq(s"An application with key[${service.application.key}] already exists")
        }
      }
    }
  }

  private def validateCanUpdate(app: Application): Seq[String] = {
    existing match {
      case None => Seq.empty
      case Some(app) => {
        ApplicationsDao.canUserUpdate(user, app) match {
          case true => Seq.empty
          case false => Seq(s"You are not authorized to update the application[${app.key}]")
        }
      }
    }
  }

  private def validateKey(): Seq[String] = {
    key match {
      case None => Seq.empty
      case Some(k) => {
        validator.service match {
          case None => Seq.empty
          case Some(service) => {
            (service.application.key == k) match {
              case true => Seq.empty
              case false => Seq(s"The key[${service.application.key}] in the uploaded file does not match the existing service key[$k]. If you would like to change the key of an application, delete the existing application and then create a new one")
            }
          }
        }
      }
    }
  }

}
