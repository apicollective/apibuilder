package controllers

import models.MainTemplate
import lib.{UrlKey, Util}
import com.gilt.apidoc.models.{Organization, User, Version, Visibility, WatchForm}
import com.gilt.apidocgenerator.models.{Service => SpecService}
import com.gilt.apidocgenerator.models.json._
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

  def redirectToLatest(orgKey: String, serviceKey: String) = Action {
    Redirect(routes.Versions.show(orgKey, serviceKey, LatestVersion))
  }

  def show(orgKey: String, serviceKey: String, versionName: String) = AnonymousOrg.async { implicit request =>
    for {
      serviceResponse <- request.api.services.getByOrgKey(orgKey = orgKey, key = Some(serviceKey))
      versionsResponse <- request.api.versions.getByOrgKeyAndServiceKey(orgKey, serviceKey)
      versionOption <- request.api.versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
      generators <- request.api.Generators.get()
      watches <- isWatching(request.api, request.user, orgKey, serviceKey)
    } yield {
      versionOption match {

        case None => {
          if (LatestVersion == versionName) {
            Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
          } else {
            Redirect(routes.Versions.show(orgKey, serviceKey, LatestVersion)).flashing("warning" -> s"Version not found: $versionName")
          }
        }

        case Some(v: Version) => {
          serviceResponse.headOption match {
            case None => {
              Redirect(routes.Versions.show(orgKey, serviceKey, LatestVersion)).flashing("warning" -> s"Service not found: ${serviceKey}")
            }
            case Some(service) => {
              Json.parse(v.json).validate[SpecService] match {
                case e: JsError => {
                  sys.error("Invalid service json: " + e)
                }
                case s: JsSuccess[SpecService] => {
                  val specService = s.get
                  val tpl = request.mainTemplate(Some(service.name + " " + v.version)).copy(
                    service = Some(service),
                    version = Some(v.version),
                    allServiceVersions = versionsResponse.map(_.version),
                    specService = Some(specService),
                    generators = generators.filter(_.enabled)
                  )
                  Ok(views.html.versions.show(tpl, specService, watches))
                }
              }
            }
          }
        }
      }
    }
  }

  def apiJson(orgKey: String, serviceKey: String, versionName: String) = AnonymousOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName).map {
      case None => {
        if (LatestVersion == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, serviceKey, LatestVersion))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        Ok(version.json).withHeaders("Content-Type" -> "application/json")
      }
    }
  }

  def postWatch(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(request.org.key, serviceKey, versionName).flatMap {
      case None => {
        if (LatestVersion == versionName) {
          Future {
            Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
          }
        } else {
          Future {
            Redirect(routes.Versions.show(orgKey, serviceKey, LatestVersion))
              .flashing("warning" -> s"Version not found: ${versionName}")
          }
        }
      }
      case Some(version) => {
        Await.result(request.api.watches.get(
          userGuid = Some(request.user.guid),
          organizationKey = Some(orgKey),
          serviceKey = Some(serviceKey)
        ), 5000.millis).headOption match {
          case None => {
            request.api.watches.post(
              WatchForm(
                userGuid = request.user.guid,
                organizationKey = orgKey,
                serviceKey = serviceKey
              )
            ).map { _ =>
              Redirect(routes.Versions.show(orgKey, serviceKey, versionName)).flashing("success" -> "You are now watching this service")
            }
          }
          case Some(watch) => {
            request.api.watches.deleteByGuid(watch.guid).map { _ =>
              Redirect(routes.Versions.show(orgKey, serviceKey, versionName)).flashing("success" -> "You are no longer watching this service")
            }
          }
        }
      }
    }
  }

  def create(orgKey: String, serviceKey: Option[String] = None) = AuthenticatedOrg.async { implicit request =>
    request.requireMember()

    serviceKey match {

      case None => Future {
        val tpl = request.mainTemplate(Some(Util.AddServiceText))
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
          serviceResponse <- request.api.Services.getByOrgKey(orgKey = orgKey, key = Some(key))
          versionsResponse <- request.api.Versions.getByOrgKeyAndServiceKey(orgKey, key, limit = Some(1))
        } yield {
          serviceResponse.headOption match {
            case None => {
              Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${key}")
            }
            case Some(service) => {
              val tpl = request.mainTemplate(Some(s"${service.name}: Upload new version")).copy(
                service = Some(service),
                version = versionsResponse.headOption.map(_.version)
              )
              val filledForm = uploadForm.fill(
                UploadData(
                  version = versionsResponse.headOption.map(v => Util.calculateNextVersion(v.version)).getOrElse(DefaultVersion),
                  visibility = service.visibility.toString
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

    val tpl = request.mainTemplate(Some("Add Service"))
    val boundForm = uploadForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.versions.form(tpl, errors))
      },

      valid => {

        request.body.file("file") match {
          case None => Future {
            Ok(views.html.versions.form(tpl, boundForm, Seq("Please select a non empty file to upload")))
          }

          case Some(file) => {
            val path = File.createTempFile("api", "json")
            file.ref.moveTo(path, true)
            val contents = scala.io.Source.fromFile(path, "UTF-8").getLines.mkString("\n")

            Json.parse(contents).asOpt[SpecService] match {
              case None => {
                Future {
                  Ok(views.html.versions.form(tpl, boundForm,
                    Seq("Invalid JSON - please make sure JSON includes at least a top level name element.")))
                }
              }

              case Some(specService) => {
                val serviceKey = UrlKey.generate(specService.name)

                request.api.Versions.putByOrgKeyAndServiceKeyAndVersion(
                  request.org.key,
                  serviceKey,
                  valid.version,
                  contents,
                  Some(Visibility(valid.visibility))
                ).map { version =>
                  Redirect(routes.Versions.show(request.org.key, serviceKey, valid.version)).flashing( "success" -> "Service description uploaded" )
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
    )
  }

  private def isWatching(
    api: com.gilt.apidoc.Client,
    user: Option[User],
    orgKey: String,
    serviceKey: String
  ): Future[Boolean] = {
    user match {
      case None => Future { false }
      case Some(u) => {
        api.watches.get(
          userGuid = Some(u.guid),
          organizationKey = Some(orgKey),
          serviceKey = Some(serviceKey)
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
