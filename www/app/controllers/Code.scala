package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, target: String) = AnonymousOrg.async { request =>
    request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndTargetKey(orgKey, serviceKey, version, target).map {
      case None => Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> "Version not found")
      case Some(r) => Ok(r.source)
    }
  }

}
