package controllers

import models.MainTemplate
import core.ServiceDescription
import client.Apidoc
import client.Apidoc.{ Organization, Service, User, Version }
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._

object Versions extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
      service <- Apidoc.services.findByOrganizationKeyAndKey(orgKey, serviceKey)
      versions <- Apidoc.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, 10)
      version <- Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      val sd = ServiceDescription(version.json.get)
      val tpl = MainTemplate(service.get.name + " " + version.version,
                             user = Some(request.user),
                             org = Some(org.get),
                             service = Some(service.get),
                             version = Some(version.version),
                             allServiceVersions = versions.map(_.version),
                             serviceDescription = Some(sd))
      Ok(views.html.versions.show(tpl, sd))
    }
  }


  def create(orgKey: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
    } yield {
      org match {
        case None => Redirect("/").flashing("warning" -> "Org not found")
        case Some(o: Organization) => Ok(views.html.versions.form(o))
      }
    }
  }

  def createPost(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgOption <- Apidoc.organizations.findByKey(orgKey)
    } yield {
      val org = orgOption.get
      versionForm.bindFromRequest.fold (

        errors => {
          // TODO: Display errors
          // Ok(views.html.versions.form(errors))
          Ok(views.html.versions.form(org))
        },

        valid => {
          sys.error("TODO: Uploaded file for org: " + org.name)
        }

      )
    }
  }

  case class VersionForm(name: String)
  private val versionForm = Form(
    mapping(
      "file" -> nonEmptyText
    )(VersionForm.apply)(VersionForm.unapply)
  )


}
