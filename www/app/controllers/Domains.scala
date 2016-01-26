package controllers

import com.bryzek.apidoc.api.v0.models.{Domain, Organization, User}
import models._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class Domains @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireMember
    val tpl = request.mainTemplate(title = Some("Domains"))
    Ok(views.html.domains.index(tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Domains))))))
  }

  def create(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate(title = Some("Add Domain"))
    Ok(views.html.domains.form(tpl, Domains.domainForm))
  }

  def postCreate(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate(title = Some("Add Domain"))
    val boundForm = Domains.domainForm.bindFromRequest
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
          case response: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
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

}

object Domains {
 
  case class DomainData(name: String)
  private[controllers] val domainForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(DomainData.apply)(DomainData.unapply)
  )
}
