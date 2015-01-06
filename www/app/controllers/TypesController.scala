package controllers

import lib.{DatatypeResolver, Href, TypeNameResolver}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.Future

object TypesController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def resolve(typeName: String) = Anonymous.async { implicit request =>
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
        ).flatMap { orgs =>
          orgs.headOption match {
            case None => {
              Future {
                Ok(views.html.types.resolve(
                  request.mainTemplate(),
                  typeName
                ))
              }
            }
            case Some(org) => {
              val versionName = "latest"
              request.api.Versions.getByOrgKeyAndApplicationKeyAndVersion(org.key, resolution.applicationKey, versionName).map { r =>
                r match {
                  case None => {
                    Ok(views.html.types.resolve(
                      request.mainTemplate(),
                      typeName
                    ))
                  }
                  case Some(v) => {
                    val service = Json.parse(v.service.toString).as[Service]

                    DatatypeResolver(
                      enumNames = service.enums.map(_.name),
                      modelNames = service.models.map(_.name)
                    ).toType(resolution.name) match {
                      case None => {
                        sys.error(s"NONE: ${resolution.name}")
                      }
                      case Some(t) => {
                        sys.error(Href(org.key, resolution.applicationKey, versionName, t).url)
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
  }

}
