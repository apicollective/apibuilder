package controllers

import io.apibuilder.api.v0.models.{ApplicationForm, Error, Organization, Original, User, Version, VersionForm, Visibility}
import io.apibuilder.api.v0.models.json._
import io.apibuilder.spec.v0.models.{Field, Service, UnionType}
import lib._
import builder.OriginalValidator
import db._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import _root_.util.ApiBuilderServiceImportResolver

@Singleton
class Versions @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  apiBuilderServiceImportResolver: ApiBuilderServiceImportResolver,
  applicationsDao: ApplicationsDao,
  databaseServiceFetcher: DatabaseServiceFetcher,
  versionsDao: VersionsDao,
  versionValidator: VersionValidator,
) extends ApibuilderController {

  private[this] val DefaultVisibility = Visibility.Organization

  def getByApplicationKey(orgKey: String, applicationKey: String, limit: Long = 25, offset: Long = 0) = Anonymous { request =>
    val versions = applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey).map { application =>
      versionsDao.findAll(
        request.authorization,
        applicationGuid = Some(application.guid),
        limit = limit,
        offset = offset
      )
    }.getOrElse(Nil)
    Ok(Json.toJson(versions))
  }

  def getByApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = Anonymous { request =>
    versionsDao.findVersion(request.authorization, orgKey, applicationKey, version) match {
      case None => NotFound
      case Some(v: Version) => Ok(Json.toJson(v))
    }
  }

  def getExampleByApplicationKeyAndVersionAndTypeName(
    orgKey: String, applicationKey: String, version: String, typeName: String, subTypeName: Option[String], optionalFields: Option[Boolean]
  ) = Anonymous { request =>
    versionsDao.findVersion(request.authorization, orgKey, applicationKey, version) match {
      case None => NotFound
      case Some(v: Version) => {

        val service = apiBuilderServiceImportResolver
          .resolve(request.authorization, v.service)
          .foldLeft(v.service) { case (service, child) =>
          val namespace = child.namespace

          def fixType[T](origTyp: String, update: String => T): T = {
            val ArrayRx = """(\[?)(.*?)(\]?)""".r
            origTyp match {
              case ArrayRx(pre, typ, pst) =>
                if (child.models.exists(_.name == typ)) {
                  update(s"$pre$namespace.models.$typ$pst")
                } else if (child.enums.exists(_.name == typ)) {
                  update(s"$pre$namespace.enums.$typ$pst")
                } else if (child.unions.exists(_.name == typ)) {
                  update(s"$pre$namespace.unions.$typ$pst")
                } else {
                  update(origTyp)
                }
              case _ => update(origTyp)
            }
          }

          def prefixUnionType(unionType: UnionType) = fixType(unionType.`type`, typ => unionType.copy(`type` = typ))
          def prefixModelFields(field: Field)       = fixType(field.`type`, typ => field.copy(`type` = typ))

          service.copy(
            enums = service.enums ++ child.enums.map(e => e.copy(name = s"$namespace.enums.${e.name}")),
            models = service.models ++ child.models.map(m => m.copy(name = s"$namespace.models.${m.name}", fields = m.fields.map(prefixModelFields))),
            unions = service.unions ++ child.unions.map(u => u.copy(name = s"$namespace.unions.${u.name}", types = u.types.map(prefixUnionType)))
          )
        }

        val example = if (optionalFields.getOrElse(false)) {
          ExampleJson.allFields(service)
        } else {
          ExampleJson.requiredFieldsOnly(service)
        }

        example.sample(typeName, subTypeName) match {
          case None => NotFound
          case Some(js) => Ok(js)
        }
      }
    }
  }

  def postByVersion(
    orgKey: String,
    versionName: String
  ) = Identified { request =>
    withOrg(request.authorization, orgKey) { org =>
      request.body match {
        case AnyContentAsJson(json) => {
          json.validate[VersionForm] match {
            case e: JsError => {
              Conflict(Json.toJson(Validation.invalidJson(e)))
            }
            case s: JsSuccess[VersionForm] => {
              val form = s.get
              OriginalValidator(
                config = toServiceConfiguration(org, versionName),
                original = OriginalUtil.toOriginal(form.originalForm),
                fetcher = databaseServiceFetcher.instance(request.authorization)
              ).validate() match {
                case Left(errors) => {
                  Conflict(Json.toJson(Validation.errors(errors)))
                }
                case Right(service) => {
                  versionValidator.validate(request.user, org, service.application.key) match {
                    case Nil => {
                      upsertVersion(request.user, org, versionName, form, OriginalUtil.toOriginal(form.originalForm), service) match {
                        case Left(errors) => Conflict(Json.toJson(errors))
                        case Right(version) => Ok(Json.toJson(version))
                      }
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

  def putByApplicationKeyAndVersion(
    orgKey: String,
    applicationKey: String,
    versionName: String
  ) = Identified { request =>
    withOrg(request.authorization, orgKey) { org =>
      request.body match {
        case AnyContentAsJson(json) => {
          json.validate[VersionForm] match {
            case e: JsError => {
              Conflict(Json.toJson(Validation.invalidJson(e)))
            }
            case s: JsSuccess[VersionForm] => {
              val form = s.get
              OriginalValidator(
                config = toServiceConfiguration(org, versionName),
                original = OriginalUtil.toOriginal(form.originalForm),
                fetcher = databaseServiceFetcher.instance(request.authorization)
              ).validate() match {
                case Left(errors) => {
                  Conflict(Json.toJson(Validation.errors(errors)))
                }
                case Right(service) => {
                  versionValidator.validate(request.user, org, service.application.key, Some(applicationKey)) match {
                    case Nil => {
                      upsertVersion(request.user, org, versionName, form, OriginalUtil.toOriginal(form.originalForm), service, Some(applicationKey)) match {
                        case Left(errors) => Conflict(Json.toJson(errors))
                        case Right(version) => Ok(Json.toJson(version))
                      }
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

  def deleteByApplicationKeyAndVersion(orgKey: String, applicationKey: String, version: String) = Identified { request =>
    withOrgMember(request.user, orgKey) { _ =>
      versionsDao.findVersion(request.authorization, orgKey, applicationKey, version).foreach { version =>
        versionsDao.softDelete(request.user, version)
      }
      NoContent
    }
  }

  private[this] def upsertVersion(
    user: User,
    org: Organization,
    versionName: String,
    form: VersionForm,
    original: Original,
    service: Service,
    applicationKey: Option[String] = None
  ): Either[Seq[Error], Version] = {
    val appResult = applicationKey.flatMap { key => applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, key) } match {
      case None => {
        val appForm = ApplicationForm(
          key = applicationKey,
          name = service.name,
          description = service.description,
          visibility = form.visibility.getOrElse(DefaultVisibility)
        )
        applicationsDao.validate(org, appForm) match {
          case Nil => Right(applicationsDao.create(user, org, appForm))
          case errors => Left(errors)
        }
      }
      case Some(app) => {
        form.visibility.map { v =>
          if (app.visibility != v) {
            applicationsDao.setVisibility(user, app, v)
          }
        }
        Right(app)
      }
    }

    appResult match {
      case Left(errors) => {
        Left(errors)
      }

      case Right(application) => {
        val version = versionsDao.findByApplicationAndVersion(Authorization.User(user.guid), application, versionName) match {
          case None => versionsDao.create(user, application, versionName, original, service)
          case Some(existing: Version) => versionsDao.replace(user, existing, application, original, service)
        }
        Right(version)
      }
    }
  }

  private[this] def toServiceConfiguration(
    org: Organization,
    version: String
  ) = ServiceConfiguration(
    orgKey = org.key,
    orgNamespace = org.namespace,
    version = version
  )

}
