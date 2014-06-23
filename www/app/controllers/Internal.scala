package controllers

import play.api._
import play.api.mvc._

object Internal extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def healthcheck() = Action.async { request =>
    for {
      orgs <- Authenticated.api.Organizations.get(limit = Some(1))
    } yield {
      Ok(views.html._internal_.healthcheck(orgs.entity.headOption))
    }
  }

}
