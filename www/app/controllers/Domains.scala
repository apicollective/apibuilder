package controllers

import apidoc.models.{ Organization, User }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Domains extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String) = AuthenticatedOrg { implicit request =>
    Ok(views.html.domains.index(
      MainTemplate(
        title = "Domains",
        org = Some(request.org),
        user = Some(request.user),
        settings = Some(SettingsMenu(section = Some(SettingSection.Domains)))
      )
    ))
  }

  def postRemove(orgKey: String, domain: String) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, s"User is not an admin of org[$orgKey]")

    for {
      //response <- request.api.Domains.deleteByOrgKeyAndDomain(orgKey, domain)
      req <- request.api.Organizations.getByKey(orgKey)
    } yield {
      Redirect(routes.Domains.index(request.org.key)).flashing("success" -> s"Domain removed")
    }
  }
 
}
