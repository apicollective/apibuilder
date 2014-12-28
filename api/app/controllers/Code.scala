package controllers

import java.util.UUID

import com.gilt.apidoc.models.{Generator, Version}
import com.gilt.apidoc.models.json._

import com.gilt.apidocgenerator.Client
import com.gilt.apidocgenerator.models.InvocationForm

import core.ServiceBuilder
import db.{GeneratorsDao, Authorization, VersionsDao}
import lib.{Config, AppConfig, Validation}

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val apidocVersion = Config.requiredString("git.version")

  def getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(
    orgKey: String,
    serviceKey: String,
    version: String,
    generatorKey: String
  ) = AnonymousRequest.async { request =>
    VersionsDao.findVersion(Authorization(request.user), orgKey, serviceKey, version) match {
      case None => {
        Future.successful(NotFound)
      }

      case Some(v) => {
        GeneratorsDao.findAll(user = request.user, key = Some(generatorKey)).headOption match {
          case None => {
            Future.successful(Conflict(Json.toJson(Validation.error(s"Generator with key[$generatorKey] not found"))))
          }

          case Some(generator: Generator) => {
            val userAgent = s"apidoc:$apidocVersion http://${AppConfig.apidocWebHostname}/${orgKey}/${serviceKey}/${v.version}/${generator.key}"
            val service = ServiceBuilder(v.json)
            new Client(generator.uri).invocations.postByKey(
              key = generator.key,
              invocationForm = InvocationForm(service = service, userAgent = Some(userAgent))
            ).map { invocation =>
              Ok(Json.toJson(com.gilt.apidoc.models.Code(generator, invocation.source)))
            }
          }
        }
      }
    }
  }

}
