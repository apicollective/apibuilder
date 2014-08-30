package controllers

import com.gilt.apidoc.models.{Organization, Service, User, Visibility}
import core.ServiceDescription
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object ServiceSettings extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private def mainTemplate(
    api: com.gilt.apidoc.Client,
    base: MainTemplate,
    serviceKey: String,
    versionName: String
  ): Future[MainTemplate] = {
    for {
      serviceResponse <- api.Services.getByOrgKey(orgKey = base.org.get.key, key = Some(serviceKey))
      versionOption <- api.Versions.getByOrgKeyAndServiceKeyAndVersion(base.org.get.key, serviceKey, versionName)
    } yield {
      val service = serviceResponse.headOption.getOrElse {
        sys.error("Service not found")
      }
      val sd = ServiceDescription(versionOption.get.json)
      base.copy(
        title = service.name + " Settings",
        service = Some(service),
        version = versionOption.map(_.version),
        serviceDescription = Some(sd)
      )
    }
  }

  def show(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(), serviceKey, versionName)
    } yield {
      Ok(views.html.service_settings.show(tpl, tpl.service.get))
    }
  }

  def edit(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      tpl <- mainTemplate(request.api, request.mainTemplate(), serviceKey, versionName)
    } yield {
      val filledForm = settingsForm.fill(Settings(visibility = tpl.service.get.visibility.toString))
      Ok(views.html.service_settings.form(tpl, filledForm))
    }
  }

  def postEdit(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    mainTemplate(request.api, request.mainTemplate(), serviceKey, versionName).flatMap { tpl =>
      val boundForm = settingsForm.bindFromRequest
      boundForm.fold (
        errors => Future {
          Ok(views.html.service_settings.form(tpl, errors))
        },

        valid => {
          val service = tpl.service.get
          request.api.Services.putByOrgKeyAndServiceKey(request.org.key, service.key, service.name, service.description, Visibility(valid.visibility)).map { Unit =>
            Redirect(routes.ServiceSettings.show(request.org.key, service.key, versionName)).flashing("success" -> s"Settings updated")
          }.recover {
            case response: com.gilt.apidoc.error.ErrorsResponse => {
              Ok(views.html.service_settings.form(tpl, boundForm, response.errors.map(_.message)))
            }
          }
        }
      )

    }
  }

  def postDelete(orgKey: String, serviceKey: String, versionName: String) = AuthenticatedOrg.async { implicit request =>
    for {
      result <- request.api.Services.deleteByOrgKeyAndServiceKey(request.org.key, serviceKey)
    } yield {
      Redirect(routes.Organizations.show(request.org.key)).flashing("success" -> s"Service $serviceKey deleted")
    }
  }

  case class Settings(visibility: String)
  private val settingsForm = Form(
    mapping(
      "visibility" -> text
    )(Settings.apply)(Settings.unapply)
  )

}
