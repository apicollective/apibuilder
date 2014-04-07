package controllers

import models.MainTemplate
import Apidoc.Version
import core.{ Organization, Service, User }
import play.api._
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration._

object Versions extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def latest(orgKey: String, serviceKey: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
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
          Ok("v: " + v.version)
        }
      }
    }
  }

  def show(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
      version <- Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      Ok("v: " + version.toString)
    }
  }

}
