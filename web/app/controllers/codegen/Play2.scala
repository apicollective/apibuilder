package controllers.codegen

import core.ServiceDescription
import controllers.Authenticated
import client.Apidoc.Version
import play.api._
import play.api.mvc._
import lib.RouteGenerator

import play.api._
import play.api.mvc._

object Play2 extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def getRoutes(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { request =>
    for {
      version <- request.client.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      version match {
        case None => {
          Redirect(controllers.routes.Organizations.show(orgKey)).flashing ("warning" -> "Service version not found")
        }

        case Some(v: Version) => {
          val generator = RouteGenerator(ServiceDescription(v.json.get))
          Ok(generator.generate())
        }

      }
    }
  }

}
