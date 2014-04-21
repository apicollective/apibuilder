package controllers

import models.MainTemplate
import core.{ ServiceDescription, ServiceDescriptionValidator, UrlKey }
import client.Apidoc
import client.Apidoc.{ Organization, Service, User, Version }
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.io.File

object Versions extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
      service <- request.client.services.findByOrganizationKeyAndKey(orgKey, serviceKey)
      versions <- request.client.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, limit = 10)
      version <- request.client.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      version match {

        case None => {
          Redirect(controllers.routes.Organizations.show(orgKey)).flashing("warning" -> s"Service version ${versionName} not found")
        }

        case Some(v: Version) => {
          val sd = ServiceDescription(v.json.get)
          val tpl = MainTemplate(service.get.name + " " + v.version,
                                 user = Some(request.user),
                                 org = Some(org.get),
                                 service = Some(service.get),
                                 version = Some(v.version),
                                 allServiceVersions = versions.map(_.version),
                                 serviceDescription = Some(sd))
          Ok(views.html.versions.show(tpl, sd))
        }
      }
    }
  }


  def create(orgKey: String, version: Option[String]) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
    } yield {
      org match {
        case None => Redirect("/").flashing("warning" -> "Org not found")
        case Some(o: Organization) => {
          val tpl = MainTemplate(title = s"${o.name}: Add Service",
                                 user = Some(request.user),
                                 org = Some(o))
          val filledForm = uploadForm.fill(UploadData(version.getOrElse("")))
          Ok(views.html.versions.form(tpl, filledForm))
        }
      }
    }
  }

  def createPost(orgKey: String) = Authenticated.async(parse.multipartFormData) { implicit request =>
    for {
      orgOption <- request.client.organizations.findByKey(orgKey)
    } yield {
      orgOption match {

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
                  val response = Await.result(request.client.versions.put(org.key, serviceKey, valid.version, path), 1000 millis)
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
