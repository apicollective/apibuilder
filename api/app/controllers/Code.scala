package controllers

import java.util.UUID

import com.gilt.apidoc.api.v0.models.{Generator, Version}
import com.gilt.apidoc.api.v0.models.json._

import com.gilt.apidoc.spec.v0.models.{Service}
import com.gilt.apidoc.spec.v0.models.json._

import com.gilt.apidoc.generator.v0.Client
import com.gilt.apidoc.generator.v0.models.InvocationForm

import db.{GeneratorsDao, Authorization, VersionsDao}
import lib.{Config, AppConfig, Validation}

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val apidocVersion = Config.requiredString("git.version")

  def getByOrgKeyAndApplicationKeyAndVersionAndGeneratorKey(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = AnonymousRequest.async { request =>
    VersionsDao.findVersion(Authorization(request.user), orgKey, applicationKey, versionName) match {
      case None => {
        Future.successful(NotFound)
      }

      case Some(version) => {
        GeneratorsDao.findAll(Authorization(request.user), key = Some(generatorKey)).headOption match {
          case None => {
            Future.successful(Conflict(Json.toJson(Validation.error(s"Generator with key[$generatorKey] not found"))))
          }

          case Some(generator: Generator) => {
            val userAgent = s"apidoc:$apidocVersion ${AppConfig.apidocWwwHost}/${orgKey}/${applicationKey}/${version.version}/${generator.key}"

            new Client(generator.uri).invocations.postByKey(
              key = generator.key,
              invocationForm = InvocationForm(service = version.service, userAgent = Some(userAgent))
            ).map { invocation =>
              Ok(Json.toJson(com.gilt.apidoc.api.v0.models.Code(generator, invocation.source)))
            }.recover {
              case r: com.gilt.apidoc.generator.v0.errors.ErrorsResponse => {
                Conflict(Json.toJson(Validation.errors(r.errors.map(_.message))))
              }
            }
          }
        }
      }
    }
  }

}
