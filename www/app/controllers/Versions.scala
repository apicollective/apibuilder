package controllers

import models.MainTemplate
import core.{ ServiceDescription, ServiceDescriptionValidator, UrlKey }
import apidoc.models.{ Organization, Version }
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.io.File

object Versions extends Controller {

  private val DefaultVersion = "0.0.1-dev"

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { implicit request =>
    val future = for {
      org <- request.api.Organizations.get(key = Some(orgKey))
      serviceResponse <- request.api.Services.getByOrgKey(orgKey = orgKey, key = Some(serviceKey))
      versionsResponse <- request.api.Versions.getByOrgKeyAndServiceKey(orgKey, serviceKey)
      versionResponse <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      println("versionResponse.entity: " + versionResponse.entity)
      versionResponse.entity match {

        case v: Version => {
          val service = serviceResponse.entity.headOption.getOrElse {
            sys.error(s"Could not find service for orgKey[$orgKey] and key[$serviceKey]")
          }
          val sd = ServiceDescription(v.json)
          val tpl = MainTemplate(service.name + " " + v.version,
                                 user = Some(request.user),
                                 org = Some(org.entity.head),
                                 service = Some(service),
                                 version = Some(v.version),
                                 allServiceVersions = versionsResponse.entity.map(_.version),
                                 serviceDescription = Some(sd))
          Ok(views.html.versions.show(tpl, sd))
        }
      }
    }

    future.recover {
      case request.api.FailedResponse(_, 404) => if ("latest" == versionName) {
        Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
      } else {
        Redirect(routes.Versions.show(orgKey, serviceKey, "latest"))
          .flashing("warning" -> s"Version not found: ${versionName}")
      }
    }
  }

  def apiJson(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { implicit request =>
    val future = for {
      versionResponse <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      val v = versionResponse.entity
      Ok(v.json).withHeaders("Content-Type" -> "application/json")
    }

    future.recover {
      case request.api.FailedResponse(_, 404) => if ("latest" == versionName) {
        Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Service not found: ${serviceKey}")
      } else {
        Redirect(routes.Versions.show(orgKey, serviceKey, "latest"))
          .flashing("warning" -> s"Version not found: ${versionName}")
      }
    }
  }


  def create(orgKey: String, version: Option[String]) = Authenticated.async { implicit request =>
    for {
      org <- request.api.Organizations.get(key = Some(orgKey))
    } yield {
      org.entity.headOption match {
        case None => Redirect("/").flashing("warning" -> "Org not found")
        case Some(o: Organization) => {
          val tpl = MainTemplate(title = s"${o.name}: Add Service",
                                 user = Some(request.user),
                                 org = Some(o))
          val filledForm = uploadForm.fill(UploadData(version.getOrElse(DefaultVersion)))
          Ok(views.html.versions.form(tpl, filledForm))
        }
      }
    }
  }

  def createPost(orgKey: String) = Authenticated.async(parse.multipartFormData) { implicit request =>
    for {
      orgOption <- request.api.Organizations.get(key = Some(orgKey))
    } yield {
      orgOption.entity.headOption match {

        case None => {
          Redirect("/").flashing("warning" -> "Org not found")
        }

        case Some(org: Organization) => {
          val tpl = MainTemplate(title = s"${org.name}: Add Service",
                                 user = Some(request.user),
                                 org = Some(org))

          val boundForm = uploadForm.bindFromRequest
          boundForm.fold (

            errors => {
              Ok(views.html.versions.form(tpl, errors))
            },

            valid => {

              request.body.file("file").map { file =>
                val path = File.createTempFile("api", "json")
                file.ref.moveTo(path, true)
                val contents = scala.io.Source.fromFile(path).getLines.mkString("\n")

                val validator = ServiceDescriptionValidator(contents)
                if (validator.isValid) {
                  val serviceKey = UrlKey.generate(validator.serviceDescription.get.name)
                  val response = Await.result(request.client.versions.put(org.key, serviceKey, valid.version, path), 5000.millis)
                  Redirect(routes.Versions.show(org.key, serviceKey, valid.version)).flashing( "success" -> "Service description uploaded" )
                } else {
                  Ok(views.html.versions.form(tpl, boundForm, validator.errors))
                }

              }.getOrElse {
                Ok(views.html.versions.form(tpl, boundForm, Seq("Please select a non empty file to upload")))
              }
            }
          )
        }
      }
    }
  }

  case class UploadData(version: String)
  private val uploadForm = Form(
    mapping(
      "version" -> nonEmptyText
    )(UploadData.apply)(UploadData.unapply)
  )

}
