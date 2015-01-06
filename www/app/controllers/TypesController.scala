package controllers

import lib.{DatatypeResolver, TypeLabel, TypeNameResolver}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.Future

object TypesController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  /**
   * typeName: e.g. com.gilt.apidoc-spec.models.service or
   * com.gilt.apidoc-spec.models.service:0.7.39
   */
  def resolve(
    typeName: String,
    versionName: String
  ) = Anonymous.async { implicit request =>
    TypeNameResolver(typeName).resolve match {
      case None => Future {
        Ok(views.html.types.resolve(
          request.mainTemplate(),
          typeName
        ))
      }
      case Some(resolution) => {
        request.api.organizations.get(
          namespace = Some(resolution.orgNamespace)
        ).map { orgs =>
          orgs.headOption match {
            case None => {
              Ok(views.html.types.resolve(
                request.mainTemplate(),
                typeName,
                Seq(s"No organization found for the namespace ${resolution.orgNamespace}")
              ))
            }
            case Some(org) => {
              Redirect(routes.Versions.show(org.key, resolution.applicationKey, versionName) + "#" + resolution.kind + "-" + resolution.name)
            }
          }
        }
      }
    }
  }

}
