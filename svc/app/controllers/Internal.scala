package controllers

import play.api._
import play.api.mvc._

object Internal extends Controller {

  def healthcheck() = Action { request =>
    Ok("healthy")
  }

}
