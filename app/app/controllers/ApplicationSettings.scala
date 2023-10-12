package controllers

import io.apibuilder.api.v0.models.{ApplicationForm, MoveForm, Visibility}

import javax.inject.Inject
import lib.ApiClientProvider
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class ApplicationSettings @Inject() (
                                      val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                      apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

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
    api: io.apibuilder.api.v0.Client,
    base: MainTemplate,
    applicationKey: String,
    versionName: String = "latest"
  ): Future[Either[String, MainTemplate]] = {
    for {
      applicationResponse <- api.Applications.get(orgKey = base.org.get.key, key = Some(applicationKey))
      versionOption <- apiClientProvider.callWith404(
        api.Versions.getByApplicationKeyAndVersion(base.org.get.key, applicationKey, versionName)
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

  def show(orgKey: String, applicationKey: String, versionName: String) = IdentifiedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      withRedirect(tpl) { t =>
        Ok(views.html.application_settings.show(t, t.application.get))
      }
    }
  }

  def edit(orgKey: String, applicationKey: String, versionName: String) = IdentifiedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName)
    } yield {
      withRedirect(tpl) { t =>
        val filledForm = ApplicationSettings.settingsForm.fill(ApplicationSettings.Settings(visibility = t.application.get.visibility.toString))
        Ok(views.html.application_settings.form(t, filledForm))
      }
    }
  }

  def postEdit(orgKey: String, applicationKey: String, versionName: String) = IdentifiedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey, versionName).flatMap { result =>
      result match {
        case Left(error) => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> error)
        }
        case Right(tpl) => {
          val boundForm = ApplicationSettings.settingsForm.bindFromRequest()
          boundForm.fold (
            errors => Future {
              Ok(views.html.application_settings.form(tpl, errors))
            },

            valid => {
              val application = tpl.application.get
              request.api.Applications.putByApplicationKey(
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
                case response: io.apibuilder.api.v0.errors.ErrorsResponse => {
                  Ok(views.html.application_settings.form(tpl, boundForm, response.errors.map(_.message)))
                }
              }
            }
          )
        }
      }
    }
  }

  def postDelete(orgKey: String, applicationKey: String) = IdentifiedOrg.async { implicit request =>
    for {
      result <- request.api.Applications.deleteByApplicationKey(request.org.key, applicationKey)
    } yield {
      Redirect(routes.Organizations.show(request.org.key)).flashing("success" -> s"Application $applicationKey deleted")
    }
  }

  def move(orgKey: String, applicationKey: String) = IdentifiedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(None), applicationKey)
    } yield {
      withRedirect(tpl) { t =>
        Ok(views.html.application_settings.move_form(t, ApplicationSettings.moveOrgForm))
      }
    }
  }

  def postMove(orgKey: String, applicationKey: String) = IdentifiedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(None), applicationKey).flatMap { result =>
      result match {
        case Left(error) => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> error)
        }
        case Right(tpl) => {
          val boundForm = ApplicationSettings.moveOrgForm.bindFromRequest()
          boundForm.fold (
            errors => Future {
              Ok(views.html.application_settings.move_form(tpl, errors))
            },

            valid => {
              val application = tpl.application.get
              request.api.applications.postMoveByApplicationKey(
                orgKey = request.org.key,
                applicationKey = application.key,
                moveForm = MoveForm(orgKey = valid.orgKey)
              ).map { app =>
                Redirect(routes.ApplicationSettings.show(app.organization.key, app.key, "latest")).flashing("success" -> s"Application moved")
              }.recover {
                case response: io.apibuilder.api.v0.errors.ErrorsResponse => {
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
