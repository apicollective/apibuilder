package controllers

import io.apibuilder.api.v0.models.Domain
import lib.ApiClientProvider
import models._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class Domains @Inject() (
                          val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                          apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String) = IdentifiedOrg { implicit request =>
    request.withMember {
      val tpl = request.mainTemplate(title = Some("Domains"))
      Ok(views.html.domains.index(tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Domains))))))
    }
  }

  def create(orgKey: String) = IdentifiedOrg { implicit request =>
    request.withAdmin {
      val tpl = request.mainTemplate(title = Some("Add Domain"))
      Ok(views.html.domains.form(tpl, Domains.domainForm))
    }
  }

  def postCreate(orgKey: String) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      val tpl = request.mainTemplate(title = Some("Add Domain"))
      val boundForm = Domains.domainForm.bindFromRequest()
      boundForm.fold(

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
            case response: io.apibuilder.api.v0.errors.ErrorsResponse => {
              Ok(views.html.domains.form(tpl, boundForm, response.errors.map(_.message)))
            }
          }
        }

      )
    }
  }

  def postRemove(orgKey: String, domain: String) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        _ <- request.api.Domains.deleteByName(orgKey, domain)
      } yield {
        Redirect(routes.Domains.index(request.org.key)).flashing("success" -> s"Domain removed")
      }
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
