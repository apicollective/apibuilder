package controllers

import models.MainTemplate
import javax.inject.Inject

class LogoutController @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents
) extends ApibuilderController {


  def logged_out = Action { implicit request =>
    Ok(views.html.logged_out(MainTemplate(requestPath = request.path)))
  }

  def logout = Action {
    Redirect("/logged_out").withNewSession
  }


}
