package controllers

import models.MainTemplate
import javax.inject.Inject

import lib.ApiClientProvider
import play.api.mvc.{Action, BaseController, ControllerComponents}

class Healthchecks @Inject() (
  val controllerComponents: ControllerComponents,
  apiClientProvider: ApiClientProvider
) extends BaseController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def index() = Action.async { implicit request =>
    for {
      orgs <- Authenticated.api().Organizations.get(key = Some("apicollective"), limit = 1)
    } yield {
      val tpl = MainTemplate(requestPath = request.path, title = Some("Healthcheck"), org = orgs.headOption)
      Ok(views.html.healthchecks.index(tpl))
    }
  }

}
