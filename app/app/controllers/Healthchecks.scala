package controllers

import models.MainTemplate
import javax.inject.Inject

import lib.ApiClientProvider

class Healthchecks @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  apiClientProvider: ApiClientProvider
) extends ApibuilderController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def index() = Anonymous.async { implicit request =>
    for {
      orgs <- request.api.organizations.get(key = Some("apicollective"), limit = 1)
    } yield {
      val tpl = MainTemplate(
        requestPath = request.path,
        title = Some("Healthcheck"),
        org = orgs.headOption
      )
      Ok(views.html.healthchecks.index(tpl))
    }
  }

}
