package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, target: String) = AuthenticatedOrg.async { request =>
    request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndTargetName(orgKey, serviceKey, version, target).map {
      case None => Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> "Version not found")
      case Some(r) => Ok(r.source)
    }
  }

}
