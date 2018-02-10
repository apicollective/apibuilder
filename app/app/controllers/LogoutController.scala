package controllers

import models.MainTemplate
import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class LogoutController @Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {


  def logged_out = Action { implicit request =>
    Ok(views.html.logged_out(MainTemplate(requestPath = request.path)))
  }

  def logout = Action {
    Redirect("/logged_out").withNewSession
  }


}
