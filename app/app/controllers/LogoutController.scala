package controllers

import models.MainTemplate
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject

class LogoutController @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {


  def logged_out: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.logged_out(MainTemplate(requestPath = request.path)))
  }

  def logout: Action[AnyContent] = Action {
    Redirect("/logged_out").withNewSession
  }


}
