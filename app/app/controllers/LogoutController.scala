package controllers

import models.MainTemplate
import javax.inject.Inject
import play.api.mvc.{BaseController, ControllerComponents}

class LogoutController @Inject() (
  val controllerComponents: ControllerComponents
) extends BaseController {


  def logged_out = Action { implicit request =>
    Ok(views.html.logged_out(MainTemplate(requestPath = request.path)))
  }

  def logout = Action {
    Redirect("/logged_out").withNewSession
  }


}
