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
    Ok(views.html.metadata.form(tpl, metadataForm))
  }

  def postEdit(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    val tpl = request.mainTemplate("Edit Metadata")
    val boundForm = metadataForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.metadata.form(tpl, errors))
      },

      valid => {
        sys.error("TODO: Post metadata:" + valid)
      }

    )

  }
 
  private val metadataForm = Form(
    mapping(
      "package_name" -> optional(text)
    )(OrganizationMetadata.apply)(OrganizationMetadata.unapply)
  )
}
