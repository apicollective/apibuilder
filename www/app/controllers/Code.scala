package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class Code @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def index(orgKey: String, applicationKey: String, version: String, generatorKey: String) = AnonymousOrg.async { implicit request =>
    lib.ApiClient.callWith404(
      request.api.Code.getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      case Some(code) => {

        // TODO: Better content negotiation here
        if (request.path.endsWith(".zip")) {
          val zipFile = lib.Zipfile.create(s"${orgKey}_${applicationKey}_${version}", code.files)
          Ok.sendFile(zipFile)
        } else {
          Ok(views.html.code.index(
            request.mainTemplate().copy(title = Some(code.generator.name + " - Files")),
            files = code.files
          ))
        }
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
