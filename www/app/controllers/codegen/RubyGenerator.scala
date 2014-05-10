package controllers.codegen

import core.ServiceDescription
import controllers.Authenticated
import client.Apidoc.Version
import play.api._
import play.api.mvc._
import core.generator.RubyGemGenerator

import play.api._
import play.api.mvc._

object RubyGenerator extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def gem(orgKey: String, serviceKey: String, versionName: String, filename: String) = Authenticated.async { request =>
    for {
      version <- request.client.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      version match {
        case None => {
          Redirect(controllers.routes.Organizations.show(orgKey)).flashing ("warning" -> "Service version not found")
        }

        case Some(v: Version) => {
          val generator = RubyGemGenerator(ServiceDescription(v.json.get))
          Ok(generator.generate())
        }

      }
    }
  }

}
