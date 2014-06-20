package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {

  def generate(organization: String, service: String, versionName: String, target: String) = Authenticated.async { request =>
    val future = for {
      versionResponse <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(organization, service, versionName)
      codeResponse <- request.api.Code.getByVersionGuidAndTargetName(versionResponse.entity.guid, target)
    } yield {
      Ok(codeResponse.entity.source)
    }

    future.recover {
      case request.api.FailedResponse(_, 404) => if ("latest" == versionName) {
        Redirect(routes.Organizations.show(organization))
          .flashing("warning" -> s"Service not found: $service")
      } else {
        Redirect(routes.Versions.show(organization, service, "latest"))
          .flashing("warning" -> s"Version not found: $versionName")
      }
    }
  }

}
