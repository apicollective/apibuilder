package controllers

import com.gilt.apidoc.models.{ Domain, Organization, User }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object Domains extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireMember
    val tpl = request.mainTemplate("Domains")
    Ok(views.html.domains.index(tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Domains))))))
  }

  def create(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate("Add Domain")
    Ok(views.html.domains.form(tpl, domainForm))
  }

  def postCreate(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate("Add Domain")
    val boundForm = domainForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.domains.form(tpl, errors))
      },

      valid => {
        request.api.Domains.post(
          orgKey = request.org.key,
          domain = Domain(valid.name)
        ).map { d =>
          Redirect(routes.Domains.index(request.org.key)).flashing("success" -> s"Domain added")
        }.recover {
          case response: com.gilt.apidoc.error.ErrorsResponse => {
            Ok(views.html.domains.form(tpl, boundForm, response.errors.map(_.message)))
          }
        }
      }

    )

  }

  def postRemove(orgKey: String, domain: String) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin

    for {
      response <- request.api.Domains.deleteByName(orgKey, domain)
    } yield {
      Redirect(routes.Domains.index(request.org.key)).flashing("success" -> s"Domain removed")
    }
  }
 
  case class DomainData(name: String)
  private val domainForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(DomainData.apply)(DomainData.unapply)
  )
}
