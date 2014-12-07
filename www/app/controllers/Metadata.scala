package controllers

import com.gilt.apidoc.models.{ Organization, OrganizationMetadata, User, Visibility }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object Metadata extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val DefaultVisibility = Visibility.Organization

  def show(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireMember
    val tpl = request.mainTemplate(Some("Metadata"))
    Ok(
      views.html.metadata.show(
        tpl.copy(settings = Some(SettingsMenu(section = Some(SettingSection.Metadata)))),
        request.org.metadata.getOrElse(OrganizationMetadata(visibility = None, packageName = None))
      )
    )
  }

  def edit(orgKey: String) = AuthenticatedOrg { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate(Some("Edit Metadata"))
    val filledForm = metadataForm.fill(toMetadata(request.org.metadata))
    Ok(views.html.metadata.form(tpl, filledForm))
  }

  def postEdit(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin
    val tpl = request.mainTemplate(Some("Edit Metadata"))
    val boundForm = metadataForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.metadata.form(tpl, errors))
      },

      valid => {
        val metadata = OrganizationMetadata(
          visibility = valid.visibility.map(Visibility(_)),
          packageName = valid.package_name
        )

        request.api.organizationMetadata.put(metadata, request.org.key).map { m =>
          Redirect(routes.Metadata.show(request.org.key)).flashing("success" -> s"Metadata updated")
        }.recover {
          case response: com.gilt.apidoc.error.ErrorsResponse => {
            Ok(views.html.metadata.form(tpl, boundForm, response.errors.map(_.message)))
          }
        }

      }

    )

  }

  private def toMetadata(md: Option[OrganizationMetadata]): Metadata = {
    md match {
      case None => {
        Metadata(
          visibility = Some(DefaultVisibility.toString),
          package_name = None
        )
      }
      case Some(m) => {
        Metadata(
          visibility = m.visibility match {
            case None => Some(DefaultVisibility.toString)
            case Some(v) => Some(v.toString)
          },
          package_name = m.packageName
        )
      }
    }
  }

  case class Metadata(
    visibility: Option[String],
    package_name: Option[String]
  )

  private val metadataForm = Form(
    mapping(
      "visibility" -> optional(text),
      "package_name" -> optional(text)
    )(Metadata.apply)(Metadata.unapply)
  )
}
