package controllers

import java.util.UUID

import com.gilt.apidoc.models.{Generator, Version}
import com.gilt.apidoc.models.json._
import com.gilt.apidocgenerator.Client
import core.ServiceDescriptionBuilder
import db.{GeneratorsDao, Authorization, OrganizationsDao, VersionsDao}
import lib.{Config, Validation}

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val apidocVersion = Config.requiredString("git.version")

  def getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(orgKey: String, serviceKey: String, version: String, generatorKey: String) = AnonymousRequest.async { request =>
    val auth = Authorization(request.user)
    OrganizationsDao.findByKey(auth, orgKey) match {
      case None =>
        Future.successful(NotFound)
      case Some(org) => {
        GeneratorsDao.findAll(user = request.user, key = Some(generatorKey)).headOption match {
          case None =>
            Future.successful(Conflict(Json.toJson(Validation.error(s"Invalid generator key[$generatorKey]."))))
          case Some(generator: Generator) =>
            VersionsDao.findVersion(auth, orgKey, serviceKey, version) match {
              case None => Future.successful(Conflict(Json.toJson(Validation.error(s"Invalid service[$serviceKey] or version[$version]"))))
              case Some(v: Version) =>
                val userAgent = s"apidoc:$apidocVersion http://www.apidoc.me/${org.key}/${serviceKey}/${v.version}/${generator.key}"
                val serviceDescription = ServiceDescriptionBuilder(v.json, org.metadata.flatMap(_.packageName), Some(userAgent))
                new Client(generator.uri).invocations.postByKey(serviceDescription, generator.key).map { invocation =>
                  Ok(Json.toJson(com.gilt.apidoc.models.Code(generator, invocation.source)))
                }
            }
        }
      }
    }
  }

}
