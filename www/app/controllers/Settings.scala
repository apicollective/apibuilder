package controllers

import apidoc.models.{ Organization, User }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Settings extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.api.Organizations.getByKey(orgKey).map {
      case None => Redirect("/").flashing("warning" -> "Organization not found")
      case Some(org: Organization) => {
        Ok(views.html.settings.index(
          MainTemplate(
            title = "Domains",
            org = Some(org),
            user = Some(request.user),
            settings = Some(SettingsMenu(section = Some(SettingSection.Domains)))
          )
        ))
      }
    }
  }
 
}
