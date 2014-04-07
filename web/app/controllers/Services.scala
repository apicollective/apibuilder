package controllers

import models.MainTemplate
import Apidoc.Version
import core.{ Organization, Service }
import play.api._
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration._

object Services extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def latest(orgKey: String, serviceKey: String) = Authenticated.async { request =>
    for {
      versions <- Apidoc.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, 1)
    } yield {
      versions.headOption match {
        case None => {
          Redirect("/").flashing(
            "warning" -> "Service does not have any versions"
          )
        }

        case Some(version: Version) => {
          Redirect(routes.Versions.show(orgKey, serviceKey, version.version))
        }
      }
    }
  }

  def resource(orgKey: String, serviceKey: String, version: String, resourceKey: String) = TODO

}
