package controllers

import java.util.UUID

import com.bryzek.apidoc.api.v0.models.json._

import com.bryzek.apidoc.spec.v0.models.json._
import com.bryzek.apidoc.spec.v0.models.Service

import com.bryzek.apidoc.generator.v0.Client
import com.bryzek.apidoc.generator.v0.models.InvocationForm

import db.generators.{GeneratorsDao, ServicesDao}
import db.{Authorization, VersionsDao}
import lib.{Config, AppConfig, Validation}

import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val apidocVersion = Config.requiredString("git.version")

  def get(
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
              case Some(gws) => {
                val userAgent = s"apidoc:$apidocVersion ${AppConfig.apidocWwwHost}/${orgKey}/${applicationKey}/${version.version}/${gws.generator.key}"

                new Client(service.uri).invocations.post(
                  key = gws.generator.key,
                  invocationForm = InvocationForm(service = version.service, userAgent = Some(userAgent))
                ).map { invocation =>
                  Ok(Json.toJson(com.bryzek.apidoc.api.v0.models.Code(
                    generator = gws,
                    files = invocation.files,
                    source = invocation.source
                  )))
                }.recover {
                  case r: com.bryzek.apidoc.generator.v0.errors.ErrorsResponse => {
                    Conflict(Json.toJson(Validation.errors(r.errors.map(_.message))))
                  }
                  case r: com.bryzek.apidoc.generator.v0.errors.FailedRequest => {
                    Conflict(Json.toJson(Validation.errors(Seq(s"Generator failed with ${r.getMessage}"))))
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
