package controllers

import io.apibuilder.api.v0.Client
import io.apibuilder.generator.v0.models.File

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject

import lib.ApiClientProvider
import play.api.mvc.Result

class Code @Inject() (
                       val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                       apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  def index(orgKey: String, applicationKey: String, version: String, generatorKey: String) = AnonymousOrg.async { implicit request =>
    apiClientProvider.callWith404(
      request.api.Code.getByGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      case Some(code) => {
        Ok(views.html.code.index(
          request.mainTemplate().copy(title = Some(code.generator.generator.name + " - Files")),
          orgKey = orgKey,
          applicationKey = applicationKey,
          version = version,
          generatorKey = generatorKey,
          files = code.files
        ))
      }
    }.recover {
      case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

  def zipFile(orgKey: String, applicationKey: String, version: String, generatorKey: String, fileName: String) = AnonymousOrg.async { implicit request =>
    withFiles(request.api, orgKey, applicationKey, version, generatorKey) { files =>
      val file = lib.Zipfile.create(fileName, files)
      Ok.sendFile(file, inline = true)
    }
  }

  def tarballFile(orgKey: String, applicationKey: String, version: String, generatorKey: String, fileName: String) = AnonymousOrg.async { implicit request =>
    withFiles(request.api, orgKey, applicationKey, version, generatorKey) { files =>
      val file = lib.TarballFile.create(fileName, files)
      Ok.sendFile(file, inline = true)
    }
  }

  def file(orgKey: String, applicationKey: String, version: String, generatorKey: String, fileName: String) = AnonymousOrg.async { implicit request =>
    withFiles(request.api, orgKey, applicationKey, version, generatorKey) { files =>
      files.find(_.name == fileName) match {
        case None => {
          Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> s"File $fileName not found")
        }

        case Some(file) => {
          Ok(file.contents)
        }
      }
    }
  }

  private def withFiles(
    api: Client, orgKey: String, applicationKey: String, version: String, generatorKey: String
  ) (
    f: Seq[File] => Result
  ) = {
    apiClientProvider.callWith404(
      api.Code.getByGeneratorKey(orgKey, applicationKey, version, generatorKey)
    ).map {
      case None => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> "Version not found")
      }

      case Some(code) => {
        f(code.files)
      }

    }.recover {
      case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
        Redirect(routes.Versions.show(orgKey, applicationKey, version)).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
