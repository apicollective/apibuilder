package controllers

import models.MainTemplate
import lib.{UrlKey, Util, VersionedName}
import com.gilt.apidoc.models.{Application, Organization, User, Version, VersionForm, Visibility, WatchForm}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import java.io.File

object Versions extends Controller {

  private val DefaultVersion = "0.0.1-dev"
  private val LatestVersion = "latest"

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirectToLatest(orgKey: String, applicationKey: String) = Action {
    Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
  }

  def show(orgKey: String, applicationKey: String, versionName: String) = AnonymousOrg.async { implicit request =>
    for {
      applicationResponse <- request.api.applications.getByOrgKey(orgKey = orgKey, key = Some(applicationKey))
      versionsResponse <- request.api.versions.getByOrgKeyAndApplicationKey(orgKey, applicationKey)
      versionOption <- request.api.versions.getByOrgKeyAndApplicationKeyAndVersion(orgKey, applicationKey, versionName)
      generators <- request.api.Generators.get()
      watches <- isWatching(request.api, request.user, orgKey, applicationKey)
    } yield {
      applicationResponse.headOption match {
        case None => {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion)).flashing("warning" -> s"Application not found: ${applicationKey}")
        }
        case Some(application) => {
          versionOption match {

            case None => {
              if (LatestVersion == versionName) {
                Redirect(routes.Versions.create(orgKey, application = Some(applicationKey))).flashing("success" -> s"Application does not yet have any versions")
              } else {
                Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion)).flashing("warning" -> s"Version not found: $versionName")
              }
            }

            case Some(v: Version) => {
              v.service.validate[Service] match {
                case e: JsError => {
                  sys.error("Invalid service json: " + e)
                }
                case s: JsSuccess[Service] => {
                  val service = s.get
                  // TODO: For updates, inculde application in the template
                  val tpl = request.mainTemplate(Some(service.name + " " + v.version)).copy(
                    version = Some(v.version),
                    allServiceVersions = versionsResponse.map(_.version),
                    service = Some(service),
                    generators = generators.filter(_.enabled)
                  )
                  Ok(views.html.versions.show(tpl, application, service, watches))
                }
              }
            }
          }
        }
      }
    }
  }

  def originalJson(orgKey: String, applicationKey: String, versionName: String) = AnonymousOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndApplicationKeyAndVersion(orgKey, applicationKey, versionName).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        Ok(version.original).withHeaders("Content-Type" -> "application/json")
      }
    }
  }

  def serviceJson(orgKey: String, applicationKey: String, versionName: String) = AnonymousOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndApplicationKeyAndVersion(orgKey, applicationKey, versionName).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Application not found: ${applicationKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, applicationKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        Ok(version.service).withHeaders("Content-Type" -> "application/json")
      }
    }
  }

  def postWatch(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndApplicationKeyAndVersion(request.org.key, applicationKey, versionName).flatMap {
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
      case Some(version) => {
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

  def create(orgKey: String, applicationKey: Option[String] = None) = AuthenticatedOrg.async { implicit request =>
    request.requireMember()

    applicationKey match {

      case None => Future {
        val tpl = request.mainTemplate(Some(Util.AddApplicationText))
        val filledForm = uploadForm.fill(
          UploadData(
            version = DefaultVersion,
            visibility = Visibility.Organization.toString
          )
        )
        Ok(views.html.versions.form(tpl, filledForm))
      }

      case Some(key) => {
        for {
          applicationResponse <- request.api.Applications.getByOrgKey(orgKey = orgKey, key = Some(key))
          versionsResponse <- request.api.Versions.getByOrgKeyAndApplicationKey(orgKey, key, limit = Some(1))
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
              val filledForm = uploadForm.fill(
                UploadData(
                  version = versionsResponse.headOption.map(v => Util.calculateNextVersion(v.version)).getOrElse(DefaultVersion),
                  visibility = application.visibility.toString
                )
              )
              Ok(views.html.versions.form(tpl, filledForm))
            }
          }
        }
      }
    }
  }

  def createPost(orgKey: String) = AuthenticatedOrg.async(parse.multipartFormData) { implicit request =>
    request.requireMember()

    val tpl = request.mainTemplate(Some(Util.AddApplicationText))
    val boundForm = uploadForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.versions.form(tpl, errors))
      },

      valid => {

        // TODO: Need to figure out how to get the service key out of
        // the form or offer a different way to upload / create
        // service versions

        request.body.file("file") match {
          case None => Future {
            Ok(views.html.versions.form(tpl, boundForm, Seq("Please select a non empty file to upload")))
          }

          case Some(file) => {
            val path = File.createTempFile("api", "json")
            file.ref.moveTo(path, true)
            val contents = scala.io.Source.fromFile(path, "UTF-8").getLines.mkString("\n")

            Json.parse(contents).asOpt[JsObject] match {
              case None => {
                Future {
                  Ok(views.html.versions.form(tpl, boundForm,
                    Seq("Invalid JSON - please make sure the file defines a JSON Object"))
                  )
                }
              }

              case Some(json) => {
                (json \ "name").asOpt[String] match {
                  case None => {
                    Future {
                      Ok(views.html.versions.form(tpl, boundForm,
                        Seq("Invalid JSON - please make sure JSON includes at least a top level name element."))
                      )
                    }
                  }
                  case Some(name) => {
                    val applicationKey = UrlKey.generate(name.trim)

                    request.api.Versions.putByOrgKeyAndApplicationKeyAndVersion(
                      orgKey = request.org.key,
                      applicationKey = applicationKey,
                      version = valid.version,
                      versionForm = VersionForm(
                        json = json,
                        Some(Visibility(valid.visibility))
                      )
                    ).map { version =>
                      Redirect(routes.Versions.show(request.org.key, applicationKey, valid.version)).flashing( "success" -> "Service uploaded" )
                    }.recover {
                      case r: com.gilt.apidoc.error.ErrorsResponse => {
                        Ok(views.html.versions.form(tpl, boundForm, r.errors.map(_.message)))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    )
  }

  private def isWatching(
    api: com.gilt.apidoc.Client,
    user: Option[User],
    orgKey: String,
    applicationKey: String
  ): Future[Boolean] = {
    user match {
      case None => Future { false }
      case Some(u) => {
        api.watches.get(
          userGuid = Some(u.guid),
          organizationKey = Some(orgKey),
          applicationKey = Some(applicationKey)
        ).map { watches =>
          watches match {
            case Nil => false
            case _ => true
          }
        }
      }
    }
  }

  case class UploadData(version: String, visibility: String)
  private val uploadForm = Form(
    mapping(
      "version" -> nonEmptyText,
      "visibility" -> nonEmptyText
    )(UploadData.apply)(UploadData.unapply)
  )

}
