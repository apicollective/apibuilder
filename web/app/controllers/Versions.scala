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
      versions <- Apidoc.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, 10)
    } yield {
      versions.headOption match {
        case None => {
          Redirect("/").flashing(
            "warning" -> "Service does not have any versions"
          )
        }

        case Some(version: Version) => {
          val v = Await.result(Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, version.version), 100 millis)
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

}
