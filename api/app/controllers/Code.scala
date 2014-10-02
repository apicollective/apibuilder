package controllers

import com.gilt.apidoc.models.Version
import core.ServiceDescriptionBuilder
import db.{Authorization, OrganizationDao, VersionDao}
import lib.Validation
import com.gilt.apidoc.models.json._
import com.gilt.apidoc.models
import core.generator.CodeGenTarget

import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current


object Code extends Controller {

  val apidocVersion = current.configuration.getString("apidoc.version").getOrElse {
    sys.error("apidoc.version is required")
  }

  def getByOrgKeyAndServiceKeyAndVersionAndTarget(orgKey: String, serviceKey: String, version: String, targetKey: String) = AnonymousRequest { request =>
    val auth = Authorization(request.user)
    OrganizationDao.findByKey(auth, orgKey) match {
      case None => NotFound
      case Some(org) => {
        CodeGenTarget.findByKey(targetKey) match {
          case None => {
            Conflict(Json.toJson(Validation.error(s"Invalid target key[$targetKey]. Must be one of: ${CodeGenTarget.Implemented.mkString(" ")}")))
          }
          case Some(target: CodeGenTarget) => {
            VersionDao.findVersion(auth, orgKey, serviceKey, version) match {
              case None => Conflict(Json.toJson(Validation.error(s"Invalid service[$serviceKey] or version[$version]")))
              case Some(v: Version) => {
                val userAgent = s"apidoc:$apidocVersion http://www.apidoc.me/${org.key}/code/${serviceKey}/${v.version}/${target.key}"
                val serviceDescription = ServiceDescriptionBuilder(v.json, org.metadata.flatMap(_.packageName), Some(userAgent))
                val source = target.generator.fold(sys.error(s"unsupported code generation for target[$target.key]"))(_.generate(serviceDescription))
                Ok(Json.toJson(models.Code(target.key, source)))
              }
            }
          }
        }
      }
    }
  }

}
