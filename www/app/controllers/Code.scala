package controllers

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

object Code extends Controller {

  def index(orgKey: String, applicationKey: String, version: String, generatorKey: String) = AnonymousOrg.async { implicit request =>
    lib.ApiClient.callWith404(
      request.api.Code.getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      case Some(code) => {
        Ok(views.html.code.index(
          request.mainTemplate().copy(title = Some(code.generator.name + " - Files")),
          files = code.files
        ))
      }
    }.recover {
      case r: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

  def file(orgKey: String, applicationKey: String, version: String, generatorKey: String, fileName: String) = AnonymousOrg.async { implicit request =>
    lib.ApiClient.callWith404(
      request.api.Code.getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      case Some(code) => {
        code.files.find(_.name == fileName) match {
          case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> s"File $fileName not found")
          case Some(file) => {
            Ok(file.contents)
          }
        }
      }
    }.recover {
      case r: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
