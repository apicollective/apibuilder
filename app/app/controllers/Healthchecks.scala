package controllers

import models.MainTemplate
import javax.inject.Inject

import lib.ApiClientProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

class Healthchecks @Inject() (
  val messagesApi: MessagesApi,
  apiClientProvider: ApiClientProvider
) extends Controller with I18nSupport {

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
