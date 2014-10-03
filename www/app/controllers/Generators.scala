package controllers

import java.util.UUID

import com.gilt.apidoc.models.Domain
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.Future

object Generators extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def list(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.requireMember
    request.api.Generators.get(orgKey).map { generators =>
      val tpl = request.mainTemplate("Generators")
      Ok(views.html.generators.index(tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Generators)))), generators))
    }
  }

  def create(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate("Add Generator")
    Ok(views.html.generators.form(tpl, generatorForm))
  }

  def postCreate(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate("Add Generator")
    val boundForm = generatorForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.generators.form(tpl, errors))
      },

      valid => {
        request.api.Generators.post(
          orgKey = request.org.key,
          name = valid.name,
          uri = valid.uri
        ).map { d =>
          Redirect(routes.Generators.list(request.org.key)).flashing("success" -> s"Generator added")
        }.recover {
          case response: com.gilt.apidoc.error.ErrorsResponse => {
            Ok(views.html.generators.form(tpl, boundForm, response.errors.map(_.message)))
          }
        }
      }

    )

  }

  def postRemove(orgKey: String, guid: UUID) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin

    for {
      response <- request.api.Generators.deleteByGuid(orgKey, guid)
    } yield {
      Redirect(routes.Generators.list(request.org.key)).flashing("success" -> s"Generator removed")
    }
  }
 
  case class GeneratorData(name: String, uri: String)
  private val generatorForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "uri" -> nonEmptyText
    )(GeneratorData.apply)(GeneratorData.unapply)
  )
}
