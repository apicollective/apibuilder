package controllers

import models.MainTemplate
import Apidoc.Version
import core.{ Organization, Service }
import play.api._
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration._

object Versions extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, version: String) = Authenticated.async { request =>
    for {
      version <- Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, version)
    } yield {
      version match {
        case None => {
          Redirect("/").flashing(
            "warning" -> "Service version does not exist"
          )
        }

        case Some(version: Version) => {
          Ok(version.json.getOrElse("Missing json"))
        }
      }
    }
  }

}
