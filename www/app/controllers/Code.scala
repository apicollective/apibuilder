package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {

  def generate(organization: String, service: String, versionName: String, target: String) = Authenticated.async { request =>
    for {
      versionResponse <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(organization, service, versionName)
      codeResponse <- request.api.Code.getByVersionGuidAndTargetName(versionResponse.entity.guid, target)
    } yield {
      Ok(codeResponse.entity.source)
    }
  }

}
