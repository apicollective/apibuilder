package controllers

import play.api._
import play.api.mvc._

object LogoutController extends Controller {

  def logged_out = Action {
    Ok(views.html.logged_out())
  }

  def logout = Action {
    Redirect("/logged_out").withNewSession
  }


}
