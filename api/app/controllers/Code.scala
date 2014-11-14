package controllers

import java.util.UUID

import com.gilt.apidoc.models.{Generator, Version}
import com.gilt.apidoc.models.json._
import com.gilt.apidocgenerator.Client
import core.ServiceDescriptionBuilder
import db.{GeneratorDao, Authorization, OrganizationDao, VersionDao}
import lib.Validation

import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current

import scala.concurrent.Future


object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val apidocVersion = current.configuration.getString("git.version").getOrElse {
    sys.error("git.version is required")
  }

  def getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(orgKey: String, serviceKey: String, version: String, generatorKey: String) = Authenticated.async { request =>
    val auth = Authorization(Some(request.user))
    OrganizationDao.findByKey(auth, orgKey) match {
      case None =>
        Future.successful(NotFound)
      case Some(org) => {
        GeneratorDao.findAll(user = request.user, key = Some(generatorKey)).headOption match {
          case None =>
            Future.successful(Conflict(Json.toJson(Validation.error(s"Invalid generator key[$generatorKey]."))))
          case Some(generator: Generator) =>
            VersionDao.findVersion(auth, orgKey, serviceKey, version) match {
              case None => Future.successful(Conflict(Json.toJson(Validation.error(s"Invalid service[$serviceKey] or version[$version]"))))
              case Some(v: Version) =>
                val userAgent = s"apidoc:$apidocVersion http://www.apidoc.me/${org.key}/code/${serviceKey}/${v.version}/${generator.key}"
                val serviceDescription = ServiceDescriptionBuilder(v.json, org.metadata.flatMap(_.packageName), Some(userAgent))
                new Client(generator.uri).invocations.postByKey(serviceDescription, generator.key).map { invocation =>
                  Ok(Json.toJson(com.gilt.apidoc.models.Code(generator.guid, invocation.source)))
                }
            }
        }
      }
    }
  }

}
