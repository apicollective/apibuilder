package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {
  def generate(organization: String,
               service: String,
               version: String,
               target: String) = Authenticated.async { request =>
    request.client.versions
      .findByOrganizationKeyAndServiceKeyAndVersion(organization,
        service, version).flatMap {
          case None => Future.successful(NotFound("Service not found"))
          case Some(version) => {
            request.api.Code.getByVersionAndTarget(version.guid,
              target).map { response =>
                Ok(response.entity.source)
              }
          }
        }
  }
}
