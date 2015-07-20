package controllers

import models.MainTemplate
import play.api._
import play.api.mvc._

class Healthchecks extends Controller {

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
