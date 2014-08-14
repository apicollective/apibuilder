package controllers

import apidoc.models.{ Organization, OrganizationMetadata, User }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object Metadata extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String) = AuthenticatedOrg { implicit request =>
    val tpl = request.mainTemplate("Metadata")
    Ok(
      views.html.metadata.show(
        tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Metadata)))),
        request.org.metadata.getOrElse(OrganizationMetadata(packageName = None))
      )
    )
  }

  def edit(orgKey: String) = AuthenticatedOrg { implicit request =>
    val tpl = request.mainTemplate("Edit Metadata")
    val filledForm = metadataForm.fill(request.org.metadata.getOrElse(OrganizationMetadata()))
    Ok(views.html.metadata.form(tpl, filledForm))
  }

  def postEdit(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    val tpl = request.mainTemplate("Edit Metadata")
    val boundForm = metadataForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.metadata.form(tpl, errors))
      },

      valid => {
        request.api.organizationMetadata.put(valid, request.org.key).map { m =>
          Redirect(routes.Metadata.show(request.org.key)).flashing("success" -> s"Metadata updated")
        }.recover {
          case response: apidoc.error.ErrorsResponse => {
            Ok(views.html.metadata.form(tpl, boundForm, response.errors.map(_.message)))
          }
        }

      }

    )

  }
 
  private val metadataForm = Form(
    mapping(
      "package_name" -> optional(text)
    )(OrganizationMetadata.apply)(OrganizationMetadata.unapply)
  )
}
