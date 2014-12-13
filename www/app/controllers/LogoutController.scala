package controllers

import models.MainTemplate
import play.api._
import play.api.mvc._

object LogoutController extends Controller {

  def logged_out = Action { implicit request =>
    Ok(views.html.logged_out(MainTemplate(requestPath = request.path)))
  }

  def logout = Action {
    Redirect("/logged_out").withNewSession
  }


}
