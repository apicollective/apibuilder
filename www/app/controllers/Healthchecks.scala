package controllers

import play.api._
import play.api.mvc._

object Healthchecks extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index() = Action.async { implicit request =>
    for {
      orgs <- Authenticated.api().Organizations.get(key = Some("gilt"), limit = Some(1))
    } yield {
      Ok(views.html.healthchecks.index(orgs.headOption))
    }
  }

}
