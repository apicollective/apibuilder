package controllers

import com.gilt.apidoc.api.v0.models.{Application, ApplicationForm, Organization, Original, User, Version, VersionForm, Visibility}
import com.gilt.apidoc.api.v0.models.json._
import com.gilt.apidoc.spec.v0.models.Service
import lib.ServiceConfiguration
import builder.OriginalValidator
import lib.{OriginalUtil, Validation}
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
  ) = Authenticated { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => {
        Conflict(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }
      case Some(org) => {
        request.body match {
          case AnyContentAsJson(json) => {
            json.validate[VersionForm] match {
              case e: JsError => {
                Conflict(Json.toJson(Validation.invalidJson(e)))
              }
              case s: JsSuccess[VersionForm] => {
                val form = s.get
                OriginalValidator(toServiceConfiguration(org, versionName), OriginalUtil.toOriginal(form.originalForm)).validate match {
                  case Left(errors) => {
                    Conflict(Json.toJson(Validation.errors(errors)))
                  }
                  case Right(service) => {
                    validateVersion(request.user, org, service.application.key) match {
                      case Nil => {
                        val version = upsertVersion(request.user, org, versionName, form, OriginalUtil.toOriginal(form.originalForm), service)
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

          case _ => {
            Conflict(Json.toJson(Validation.invalidJsonDocument()))
          }

        }
      }
    }
  }

  def putByOrgKeyAndApplicationKeyAndVersion(
    orgKey: String,
    applicationKey: String,
    versionName: String
  ) = Authenticated { request =>
    OrganizationsDao.findByUserAndKey(request.user, orgKey) match {
      case None => {
        Conflict(Json.toJson(Validation.error(s"Organization[$orgKey] does not exist or you are not authorized to access it")))
      }

      case Some(org: Organization) => {
        request.body match {
          case AnyContentAsJson(json) => {
            json.validate[VersionForm] match {
              case e: JsError => {
                Conflict(Json.toJson(Validation.invalidJson(e)))
              }
              case s: JsSuccess[VersionForm] => {
                val form = s.get
                OriginalValidator(toServiceConfiguration(org, versionName), OriginalUtil.toOriginal(form.originalForm)).validate match {
                  case Left(errors) => {
                    Conflict(Json.toJson(Validation.errors(errors)))
                  }
                  case Right(service) => {
                    validateVersion(request.user, org, service.application.key, Some(applicationKey)) match {
                      case Nil => {
                        val version = upsertVersion(request.user, org, versionName, form, OriginalUtil.toOriginal(form.originalForm), service, Some(applicationKey))
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
          case _ => {
            Conflict(Json.toJson(Validation.invalidJsonDocument()))
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

  private def toServiceConfiguration(
    org: Organization,
    version: String
  ) = ServiceConfiguration(
    orgKey = org.key,
    orgNamespace = org.namespace,
    version = version
  )

  private def validateVersion(
    user: User,
    org: Organization,
    applicationKey: String,
    existingKey: Option[String] = None
  ): Seq[String] = {
    VersionValidator(
      user = user,
      org = org,
      newApplicationKey = applicationKey,
      existingApplicationKey = existingKey
    ).validate
  }

}
