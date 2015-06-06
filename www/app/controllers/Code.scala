package controllers

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

object Code extends Controller {

  def generate(orgKey: String, applicationKey: String, version: String, generatorKey: String) = AnonymousOrg.async { request =>
    lib.ApiClient.callWith404(
      request.api.Code.getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      case Some(r) => Ok(r.source)
    }.recover {
      case r: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
