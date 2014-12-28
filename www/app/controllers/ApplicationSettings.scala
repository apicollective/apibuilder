package controllers

import com.gilt.apidoc.models.{Application, ApplicationForm, Organization, User, Visibility}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import scala.concurrent.Future

object ApplicationSettings extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private def mainTemplate(
    api: com.gilt.apidoc.Client,
    base: MainTemplate,
    applicationKey: String,
    versionName: String
  ): Future[MainTemplate] = {
    for {
      applicationResponse <- api.Applications.getByOrgKey(orgKey = base.org.get.key, key = Some(applicationKey))
      versionOption <- api.Versions.getByOrgKeyAndApplicationKeyAndVersion(base.org.get.key, applicationKey, versionName)
    } yield {
      val application = applicationResponse.headOption.getOrElse {
        sys.error("Application not found")
      }
      base.copy(
        title = Some(application.name + " Settings"),
        application = Some(application),
        version = versionOption.map(_.version),
        service = versionOption.flatMap(_.json.asOpt[Service])
      )
    }
  }

  def show(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      Ok(views.html.application_settings.show(tpl, tpl.application.get))
    }
  }

  def edit(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      val filledForm = settingsForm.fill(Settings(visibility = tpl.application.get.visibility.toString))
      Ok(views.html.application_settings.form(tpl, filledForm))
    }
  }

  def postEdit(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName).flatMap { tpl =>
      val boundForm = settingsForm.bindFromRequest
      boundForm.fold (
        errors => Future {
          Ok(views.html.application_settings.form(tpl, errors))
        },

        valid => {
          val application = tpl.application.get
          request.api.Applications.putByOrgKeyAndApplicationKey(
            orgKey = request.org.key,
            applicationKey = application.key,
            applicationForm = ApplicationForm(
              name = application.name.trim,
              description = application.description.map(_.trim),
              visibility = Visibility(valid.visibility)
            )
          ).map { app =>
            Redirect(routes.ApplicationSettings.show(request.org.key, application.key, versionName)).flashing("success" -> s"Settings updated")
          }.recover {
            case response: com.gilt.apidoc.error.ErrorsResponse => {
              Ok(views.html.application_settings.form(tpl, boundForm, response.errors.map(_.message)))
            }
          }
        }
      )

    }
  }

  def postDelete(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      result <- request.api.Applications.deleteByOrgKeyAndApplicationKey(request.org.key, applicationKey)
    } yield {
      Redirect(routes.Organizations.show(request.org.key)).flashing("success" -> s"Application $applicationKey deleted")
    }
  }

  case class Settings(visibility: String)
  private val settingsForm = Form(
    mapping(
      "visibility" -> text
    )(Settings.apply)(Settings.unapply)
  )

}
