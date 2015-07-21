package controllers

import java.util.UUID

import com.bryzek.apidoc.api.v0.models.json._

import com.bryzek.apidoc.spec.v0.models.Service
import com.bryzek.apidoc.spec.v0.models.json._

import com.bryzek.apidoc.generator.v0.Client
import com.bryzek.apidoc.generator.v0.models.InvocationForm

import db.{Authorization, VersionsDao}
import db.generators.{GeneratorsDao, ServicesDao}
import lib.{Config, AppConfig, Validation}

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val apidocVersion = Config.requiredString("git.version")

  def getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = AnonymousRequest.async { request =>
    VersionsDao.findVersion(request.authorization, orgKey, applicationKey, versionName) match {
      case None => {
        Future.successful(NotFound)
      }

      case Some(version) => {
        ServicesDao.findAll(request.authorization, generatorKey = Some(generatorKey)).headOption match {
          case None => {
            Future.successful(Conflict(Json.toJson(Validation.error(s"Service with generator key[$generatorKey] not found"))))
          }

          case Some(service) => {
            GeneratorsDao.findAll(request.authorization, key = Some(generatorKey)).headOption match {
              case None => {
                Future.successful(Conflict(Json.toJson(Validation.error(s"Generator with key[$generatorKey] not found"))))
              }
              case Some(generator) => {
                val userAgent = s"apidoc:$apidocVersion ${AppConfig.apidocWwwHost}/${orgKey}/${applicationKey}/${version.version}/${generator.key}"

                new Client(service.uri).invocations.postByKey(
                  key = generator.key,
                  invocationForm = InvocationForm(service = version.service, userAgent = Some(userAgent))
                ).map { invocation =>
                  val zipFile = _root_.util.Zipfile.create(s"${orgKey}_${applicationKey}_${version.version}", invocation.files)
                  println("ZIP: " + zipFile)
                  Ok(Json.toJson(com.bryzek.apidoc.api.v0.models.Code(
                    generator = generator,
                    files = invocation.files,
                    source = invocation.source
                  )))
                }.recover {
                  case r: com.bryzek.apidoc.generator.v0.errors.ErrorsResponse => {
                    Conflict(Json.toJson(Validation.errors(r.errors.map(_.message))))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}
