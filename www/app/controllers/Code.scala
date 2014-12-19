package controllers

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, generatorKey: String) = AnonymousOrg.async { request =>
    request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(orgKey, serviceKey, version, generatorKey).map {
      case None => Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> "Version not found")
      case Some(r) => Ok(r.source)
    }.recover {
      case r: com.gilt.apidoc.error.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, serviceKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
