package controllers

import com.bryzek.apidoc.api.v0.models.{Application, ApplicationForm, MoveForm, Organization, User, Visibility}
import com.bryzek.apidoc.spec.v0.models.Service
import com.bryzek.apidoc.spec.v0.models.json._
import models._

import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller, Result}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import scala.concurrent.Future

class ApplicationSettings @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private[this] def withRedirect(
    result: Either[String, MainTemplate]
  ) (
    f: MainTemplate => Result
  ): Result = {
    result match {
      case Left(error) => {
        Redirect(routes.ApplicationController.index()).flashing("warning" -> error)
      }
      case Right(t) => {
        f(t)
      }
    }
  }

  private[this] def mainTemplate(
    api: com.bryzek.apidoc.api.v0.Client,
    base: MainTemplate,
    applicationKey: String,
    versionName: String = "latest"
  ): Future[Either[String, MainTemplate]] = {
    for {
      applicationResponse <- api.Applications.getByOrgKey(orgKey = base.org.get.key, key = Some(applicationKey))
      versionOption <- lib.ApiClient.callWith404(
        api.Versions.getByOrgKeyAndApplicationKeyAndVersion(base.org.get.key, applicationKey, versionName)
      )
    } yield {
      applicationResponse.headOption match {
        case None => {
          Left("Application not found")
        }
        case Some(app) => {
          Right(
            base.copy(
              title = Some(s"${app.name} Settings"),
              application = Some(app),
              version = versionOption.map(_.version),
              service = versionOption.map(_.service)
            )
          )
        }
      }
    }
  }

  def show(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      withRedirect(tpl) { t =>
        Ok(views.html.application_settings.show(t, t.application.get))
      }
    }
  }

  def edit(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      withRedirect(tpl) { t =>
        val filledForm = ApplicationSettings.settingsForm.fill(ApplicationSettings.Settings(visibility = t.application.get.visibility.toString))
        Ok(views.html.application_settings.form(t, filledForm))
      }
    }
  }

  def postEdit(orgKey: String, applicationKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName).flatMap { result =>
      result match {
        case Left(error) => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> error)
        }
        case Right(tpl) => {
          val boundForm = ApplicationSettings.settingsForm.bindFromRequest
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
                case response: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
                  Ok(views.html.application_settings.form(tpl, boundForm, response.errors.map(_.message)))
                }
              }
            }
          )
        }
      }
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
      withRedirect(tpl) { t =>
        Ok(views.html.application_settings.move_form(t, ApplicationSettings.moveOrgForm))
      }
    }
  }

  def postMove(orgKey: String, applicationKey: String) = AuthenticatedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey).flatMap { result =>
      result match {
        case Left(error) => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> error)
        }
        case Right(tpl) => {
          val boundForm = ApplicationSettings.moveOrgForm.bindFromRequest
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
                case response: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
                  Ok(views.html.application_settings.move_form(tpl, boundForm, response.errors.map(_.message)))
                }
              }
            }
          )
        }
      }
    }
  }

}

object ApplicationSettings {

  case class Settings(visibility: String)
  private[controllers] val settingsForm = Form(
    mapping(
      "visibility" -> text
    )(Settings.apply)(Settings.unapply)
  )

  case class MoveOrgData(orgKey: String)
  private[controllers] val moveOrgForm = Form(
    mapping(
      "org_key" -> text
    )(MoveOrgData.apply)(MoveOrgData.unapply)
  )

}
