package controllers

import models.MainTemplate
import Apidoc.Version
import core.{ Organization, Service, ServiceDescription, User }
import play.api._
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration._

object Versions extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def latest(orgKey: String, serviceKey: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
      service <- Apidoc.services.findByOrganizationKeyAndKey(orgKey, serviceKey)
      versions <- Apidoc.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, 1)
    } yield {
      versions.headOption match {
        case None => {
          Redirect("/").flashing(
            "warning" -> "Service does not have any versions"
          )
        }

        case Some(version: Version) => {
          val v = Await.result(Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, version.version), 100 millis)
          val tpl = MainTemplate(service.get.name + " " + v.version,
                                 user = Some(request.user),
                                 org = Some(org.get))
          val sd = ServiceDescription(v.json.get)
          Ok(views.html.versions.show(tpl, sd))
        }
      }
    }
  }

  def show(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
      service <- Apidoc.services.findByOrganizationKeyAndKey(orgKey, serviceKey)
      version <- Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      Ok("v: " + version.toString)
    }
  }

}
