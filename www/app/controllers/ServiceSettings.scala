package controllers

import com.gilt.apidoc.models.{Service, User}
import core.ServiceDescription
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object ServiceSettings extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      serviceResponse <- request.api.Services.getByOrgKey(orgKey = orgKey, key = Some(serviceKey))
      versionOption <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      serviceResponse.headOption match {
        case None => {
          Redirect(routes.Organizations.show(orgKey)).flashing("warnings" -> "Service not found")
        }
        case Some(service) => {
          val sd = ServiceDescription(versionOption.get.json)
          val tpl = MainTemplate(
            service.name + " Settings",
            user = Some(request.user),
            org = Some(request.org),
            service = Some(service),
            version = versionOption.map(_.version),
            serviceDescription = Some(sd)
          )
          Ok(views.html.service_settings.show(tpl, service))
        }
      }
    }
  }

}
