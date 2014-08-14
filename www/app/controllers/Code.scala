package controllers

import apidoc.models.Target
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, target: String) = AuthenticatedOrg.async { request =>
    Target.fromString(target) match {
      case None => Future {
        Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> "Invalid target for code generation")
      }
      case Some(target) => {
        request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndTarget(orgKey, serviceKey, version, target).map {
          case None => Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> "Version not found")
          case Some(r) => Ok(r.source)
        }
      }
    }
  }

}
