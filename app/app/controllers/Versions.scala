package controllers

import java.io.File
import javax.inject.Inject
import io.apibuilder.api.v0.models.*
import io.apibuilder.spec.v0.models.json.*
import lib.{ApiClientProvider, FileUtils, Labels, VersionTag}
import play.api.data.Forms.*
import play.api.data.*
import play.api.libs.Files
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, MultipartFormData}

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class Versions @Inject() (
                           val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                           apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private val DefaultVersion = "0.0.1-dev"
  private val LatestVersion = "latest"

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirectToLatest(orgKey: String, applicationKey: String): Action[AnyContent] = Action {
    Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
  }

  def show(orgKey: String, applicationKey: String, versionName: String): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    for {
      applicationResponse <- request.api.applications.get(orgKey = orgKey, key = Some(applicationKey))
      versionsResponse <- request.api.versions.getByApplicationKey(orgKey, applicationKey)
      versionOption <- apiClientProvider.callWith404(
        request.api.versions.getByApplicationKeyAndVersion(orgKey, applicationKey, versionName)
      )
      generators <- request.api.generatorWithServices.get()
      watches <- isWatching(request.api, request.user, orgKey, applicationKey)
    } yield {
      applicationResponse.headOption match {
        case None => {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        }
        case Some(application) => {
          versionOption match {

            case None => {
              if (LatestVersion == versionName) {
                if (request.requestData.isMember) {
                  Redirect(routes.Versions.create(orgKey, application = Some(applicationKey))).flashing("success" -> s"Application does not yet have any versions")
                } else {
                  Redirect(routes.Organizations.show(orgKey)).flashing("success" -> s"Application does not have any versions")
                }
              } else {
                Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion)).flashing("warning" -> s"Version not found: $versionName")
              }
            }

            case Some(v: Version) => {
              // TODO: For updates, include application in the template
              val tpl = request.mainTemplate(Some(v.service.name + " " + v.version)).copy(
                version = Some(v.version),
                allServiceVersions = versionsResponse.map(_.version),
                service = Some(v.service),
                versionObject = Some(v),
                generators = generators
              )
              Ok(views.html.versions.show(tpl, application, v.service, watches)).withHeaders(
                CACHE_CONTROL -> "max-age=0, s-maxage=0"
              )
            }
          }
        }
      }
    }
  }

  def original(orgKey: String, applicationKey: String, versionName: String): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    apiClientProvider.callWith404(
      request.api.versions.getByApplicationKeyAndVersion(orgKey, applicationKey, versionName)
    ).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        version.original match {
          case None => {
            Redirect(routes.Versions.show(orgKey, applicationKey, versionName))
              .flashing("warning" -> s"Original not available")
          }
          case Some(original) => {
            original.`type` match {
              case OriginalType.ApiJson | OriginalType.Swagger | OriginalType.ServiceJson => {
                Ok(original.data).withHeaders("Content-Type" -> "application/json")
              }
              case OriginalType.AvroIdl => {
                Ok(original.data).withHeaders("Content-Type" -> "text/plain")
              }
              case OriginalType.UNDEFINED(_) => {
                Ok(original.data).withHeaders("Content-Type" -> "text/plain")
              }
            }
          }
        }
      }
    }
  }

  def example(
    orgKey: String, applicationKey: String, versionName: String, typeName: String, subTypeName: Option[String], optionalFields: Option[Boolean]
  ): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    apiClientProvider.callWith404(
      request.api.versions.getExampleByApplicationKeyAndVersionAndTypeName(orgKey, applicationKey, versionName, typeName, subTypeName = subTypeName, optionalFields = optionalFields)
    ).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }

      case Some(example) => {
        Ok(example)
      }
    }
  }

  def serviceJson(orgKey: String, applicationKey: String, versionName: String): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    apiClientProvider.callWith404(
      request.api.versions.getByApplicationKeyAndVersion(orgKey, applicationKey, versionName)
    ).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        Ok(Json.toJson(version.service)).withHeaders("Content-Type" -> "application/json")
      }
    }
  }

  def postDelete(orgKey: String, applicationKey: String, versionName: String): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    for {
      result <- apiClientProvider.callWith404(
        request.api.versions.deleteByApplicationKeyAndVersion(orgKey, applicationKey, versionName)
      )
    } yield {
      result match {
        case None => Redirect(routes.Versions.show(orgKey, applicationKey, versionName)).flashing("success" -> s"Version $versionName was not found or could not be deleted")
        case Some(_) => Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion)).flashing("success" -> s"Version $versionName deleted")
      }
    }
  }


  def postWatch(orgKey: String, applicationKey: String, versionName: String): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    apiClientProvider.callWith404(
      request.api.versions.getByApplicationKeyAndVersion(request.org.key, applicationKey, versionName)
    ).flatMap {
      case None => {
        if (LatestVersion == versionName) {
          Future {
            Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
          }
        } else {
          Future {
            Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
              .flashing("warning" -> s"Version not found: ${versionName}")
          }
        }
      }
      case Some(_) => {
        Await.result(request.api.watches.get(
          userGuid = Some(request.user.guid),
          organizationKey = Some(orgKey),
          applicationKey = Some(applicationKey)
        ), 5000.millis).headOption match {
          case None => {
            request.api.watches.post(
              WatchForm(
                userGuid = request.user.guid,
                organizationKey = orgKey,
                applicationKey = applicationKey
              )
            ).map { _ =>
              Redirect(routes.Versions.show(orgKey, applicationKey, versionName)).flashing("success" -> "You are now watching this application")
            }
          }
          case Some(watch) => {
            request.api.watches.deleteByGuid(watch.guid).map { _ =>
              Redirect(routes.Versions.show(orgKey, applicationKey, versionName)).flashing("success" -> "You are no longer watching this application")
            }
          }
        }
      }
    }
  }

  def create(
    orgKey: String,
    applicationKey: Option[String] = None
  ): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    request.withMember {

      applicationKey match {

        case None => Future {
          val tpl = request.mainTemplate(Some(Labels.AddApplicationText))
          val filledForm = Versions.uploadForm.fill(
            Versions.UploadData(
              version = DefaultVersion,
              visibility = Visibility.Organization.toString,
              originalType = None
            )
          )
          Ok(views.html.versions.form(tpl, applicationKey, filledForm))
        }

        case Some(key) => {
          for {
            applicationResponse <- request.api.Applications.get(orgKey = orgKey, key = Some(key))
            versionsResponse <- request.api.versions.getByApplicationKey(orgKey, key, limit = 1)
          } yield {
            applicationResponse.headOption match {
              case None => {
                Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${key}")
              }
              case Some(application) => {
                val tpl = request.mainTemplate(Some(s"${application.name}: Upload new version")).copy(
                  application = Some(application),
                  version = versionsResponse.headOption.map(_.version)
                )
                val filledForm = Versions.uploadForm.fill(
                  Versions.UploadData(
                    version = versionsResponse.headOption.map(v => VersionTag(v.version).nextMicro().getOrElse(v.version)).getOrElse(DefaultVersion),
                    visibility = application.visibility.toString,
                    originalType = None
                  )
                )

                val isFirstVersion = versionsResponse.isEmpty
                Ok(views.html.versions.form(tpl, applicationKey, filledForm, isFirstVersion = Some(isFirstVersion)))
              }
            }
          }
        }
      }
    }
  }

  def createPost(
    orgKey: String,
    applicationKey: Option[String] = None
  ): Action[MultipartFormData[Files.TemporaryFile]] = IdentifiedOrg.async(parse.multipartFormData) { implicit request =>
    request.withMember {
      val tpl = applicationKey match {
        case None => request.mainTemplate(Some(Labels.AddApplicationText))
        case Some(_) => request.mainTemplate(Some("Upload New Version"))
      }
      val boundForm = Versions.uploadForm.bindFromRequest()
      boundForm.fold(

        errors => Future {
          Ok(views.html.versions.form(tpl, applicationKey, errors))
        },

        valid => {

          request.body.file("file") match {
            case None => Future {
              Ok(views.html.versions.form(tpl, applicationKey, boundForm, Seq("Please select a non empty file to upload")))
            }

            case Some(file) => {
              val path = File.createTempFile("api", "json")
              file.ref.moveTo(path, replace = true)
              val versionForm = VersionForm(
                originalForm = OriginalForm(
                  `type` = valid.originalType.map(OriginalType(_)),
                  data = FileUtils.readToString(path),
                ),
                Some(Visibility(valid.visibility))
              )

              applicationKey match {
                case None => {
                  request.api.versions.postByVersion(
                    orgKey = request.org.key,
                    version = valid.version,
                    versionForm = versionForm
                  ).map { version =>
                    Redirect(routes.Versions.show(version.organization.key, version.application.key, version.version)).flashing("success" -> "Application version updated")
                  }.recover {
                    case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
                      Ok(views.html.versions.form(tpl, applicationKey, boundForm, r.errors.map(_.message)))
                    }
                  }
                }

                case Some(key) => {
                  request.api.versions.putByApplicationKeyAndVersion(
                    orgKey = request.org.key,
                    applicationKey = key,
                    version = valid.version,
                    versionForm = versionForm
                  ).map { version =>
                    Redirect(routes.Versions.show(version.organization.key, version.application.key, version.version)).flashing("success" -> "Application version created")
                  }.recover {
                    case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
                      Ok(views.html.versions.form(tpl, applicationKey, boundForm, r.errors.map(_.message)))
                    }
                  }
                }
              }
            }
          }
        }
      )
    }
  }

  private def isWatching(
    api: io.apibuilder.api.v0.Client,
    user: Option[User],
    orgKey: String,
    applicationKey: String
  ): Future[Boolean] = {
    user match {
      case None => {
        Future.successful(false)
      }

      case Some(u) => {
        api.watches.get(
          userGuid = Some(u.guid),
          organizationKey = Some(orgKey),
          applicationKey = Some(applicationKey)
        ).map(_.nonEmpty)
      }
    }
  }

}

object Versions {

  case class UploadData(
    version: String,
    visibility: String,
    originalType: Option[String]
  )

  object UploadData {
    def unapply(d: UploadData): Option[(String, String, Option[String])] = {
      Some((d.version, d.visibility, d.originalType))
    }
  }

  private[controllers] val uploadForm = Form(
    mapping(
      "version" -> nonEmptyText,
      "visibility" -> nonEmptyText,
      "original_type" -> optional(nonEmptyText)
    )(UploadData.apply)(UploadData.unapply)
  )

}
