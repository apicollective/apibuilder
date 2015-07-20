package controllers

import models.MainTemplate
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class Healthchecks @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index() = Action.async { implicit request =>
    for {
      orgs <- Authenticated.api().Organizations.get(key = Some("gilt"), limit = 1)
    } yield {
      val tpl = MainTemplate(requestPath = request.path, title = Some("Healthcheck"), org = orgs.headOption)
      Ok(views.html.healthchecks.index(tpl))
    }
  }

}
