package controllers

import com.gilt.apidoc.api.v0.models.{Application, ApplicationForm, MoveForm, Organization, User, Visibility}
import com.gilt.apidoc.spec.v0.models.Service
import com.gilt.apidoc.spec.v0.models.json._
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import scala.concurrent.Future

object ApplicationSettings extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private[this] def mainTemplate(
    api: com.gilt.apidoc.api.v0.Client,
    base: MainTemplate,
    applicationKey: String,
    versionName: String = "latest"
  ): Future[MainTemplate] = {
    for {
      applicationResponse <- api.Applications.getByOrgKey(orgKey = base.org.get.key, key = Some(applicationKey))
      versionOption <- lib.ApiClient.callWith404(
        api.Versions.getByOrgKeyAndApplicationKeyAndVersion(base.org.get.key, applicationKey, versionName)
      )
    } yield {
      applicationResponse.headOption match {
        case None => {
          sys.error("TODO: Figure out redirect as we could not find the application here")
        }
        case Some(app) => {
          base.copy(
            title = Some(s"${app.name} Settings"),
            application = Some(app),
            version = versionOption.map(_.version),
            service = versionOption.map(_.service)
          )
        }
      }
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
            case response: com.gilt.apidoc.api.v0.errors.ErrorsResponse => {
              Ok(views.html.application_settings.form(tpl, boundForm, response.errors.map(_.message)))
            }
          }
        }
      )

    }
  }

  def postDelete(orgKey: String, applicationKey: String) = AuthenticatedOrg.async { implicit request =>
    for {
      result <- request.api.Applications.deleteByOrgKeyAndApplicationKey(request.org.key, applicationKey)
    } yield {
      Redirect(routes.Organizations.show(request.org.key)).flashing("success" -> s"Application $applicationKey deleted")
    }
  }

  def move(orgKey: String, applicationKey: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey)
    } yield {
      Ok(views.html.application_settings.move_form(tpl, moveOrgForm))
    }
  }

  def postMove(orgKey: String, applicationKey: String) = AuthenticatedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey).flatMap { tpl =>
      val boundForm = moveOrgForm.bindFromRequest
      boundForm.fold (
        errors => Future {
          Ok(views.html.application_settings.move_form(tpl, errors))
        },

        valid => {
          val application = tpl.application.get
          request.api.applications.postMoveByOrgKeyAndApplicationKey(
            orgKey = request.org.key,
            applicationKey = application.key,
            moveForm = MoveForm(orgKey = valid.orgKey)
          ).map { app =>
            Redirect(routes.ApplicationSettings.show(app.organization.key, app.key, "latest")).flashing("success" -> s"Application moved")
          }.recover {
            case response: com.gilt.apidoc.api.v0.errors.ErrorsResponse => {
              Ok(views.html.application_settings.move_form(tpl, boundForm, response.errors.map(_.message)))
            }
          }
        }
      )
    }
  }

  case class Settings(visibility: String)
  private[this] val settingsForm = Form(
    mapping(
      "visibility" -> text
    )(Settings.apply)(Settings.unapply)
  )

  case class MoveOrgData(orgKey: String)
  private[this] val moveOrgForm = Form(
    mapping(
      "org_key" -> text
    )(MoveOrgData.apply)(MoveOrgData.unapply)
  )

}
