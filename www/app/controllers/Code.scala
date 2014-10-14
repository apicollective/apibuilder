package controllers

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, generator: UUID) = AnonymousOrg.async { request =>
    request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndGeneratorGuid(orgKey, serviceKey, version, generator).map {
      case None => Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> "Version not found")
      case Some(r) => Ok(r.source)
    }
  }

}
