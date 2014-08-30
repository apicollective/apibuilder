package controllers

import models.MainTemplate
import core.{ServiceDescription, ServiceDescriptionValidator, UrlKey}
import lib.Util
import com.gilt.apidoc.models.{Organization, Version, Visibility}
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import java.io.File

object Versions extends Controller {

  private val DefaultVersion = "0.0.1-dev"

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      serviceResponse <- request.api.Services.getByOrgKey(orgKey = orgKey, key = Some(serviceKey))
      versionsResponse <- request.api.Versions.getByOrgKeyAndServiceKey(orgKey, serviceKey)
      versionOption <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      versionOption match {

        case None => {
          if ("latest" == versionName) {
            Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
          } else {
            Redirect(routes.Versions.show(orgKey, serviceKey, "latest"))
              .flashing("warning" -> s"Version not found: $versionName")
          }
        }

        case Some(v: Version) => {
          val service = serviceResponse.headOption.getOrElse {
            sys.error(s"Could not find service for orgKey[$orgKey] and key[$serviceKey]")
          }

          val sd = ServiceDescription(v.json)
          val tpl = request.mainTemplate(service.name + " " + v.version).copy(
            service = Some(service),
            version = Some(v.version),
            allServiceVersions = versionsResponse.map(_.version),
            serviceDescription = Some(sd)
          )
          Ok(views.html.versions.show(tpl, sd))
        }
      }
    }
  }

  def apiJson(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName).map {
      case None => {
        if ("latest" == versionName) {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
        } else {
          Redirect(routes.Versions.show(orgKey, serviceKey, "latest"))
            .flashing("warning" -> s"Version not found: ${versionName}")
        }
      }
      case Some(version) => {
        Ok(version.json).withHeaders("Content-Type" -> "application/json")
      }
    }
  }

  def create(orgKey: String, serviceKey: Option[String] = None) = AuthenticatedOrg.async { implicit request =>
    request.requireMember()

    serviceKey match {

      case None => Future {
        val tpl = request.mainTemplate("Add service")
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
              val tpl = request.mainTemplate(s"${service.name}: Upload new version").copy(
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

    val tpl = request.mainTemplate("Add Service")
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
            val contents = scala.io.Source.fromFile(path).getLines.mkString("\n")

            val validator = ServiceDescriptionValidator(contents)
            if (validator.isValid) {
              val serviceKey = UrlKey.generate(validator.serviceDescription.get.name)
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
            } else {
              Future {
                Ok(views.html.versions.form(tpl, boundForm, validator.errors))
              }
            }
          }
        }
      }
    )
  }

  case class UploadData(version: String, visibility: String)
  private val uploadForm = Form(
    mapping(
      "version" -> nonEmptyText,
      "visibility" -> nonEmptyText
    )(UploadData.apply)(UploadData.unapply)
  )

}
