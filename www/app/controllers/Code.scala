package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {

  def generate(orgKey: String, serviceKey: String, version: String, target: String) = AuthenticatedOrg.async { request =>
    request.api.Code.getByOrgKeyAndServiceKeyAndVersionAndTargetName(orgKey, serviceKey, version, target).map { r =>
      Ok(r.entity.source)
    }.recover {
      case apidoc.FailedResponse(errors: Seq[apidoc.models.Error], 409) => {
        Redirect(routes.Organizations.show(orgKey))
          .flashing("warning" -> errors.map(_.message).mkString(". "))
      }
    }
  }

}
