package controllers

import play.api.mvc._

class AccountController extends Controller {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AccountController.index())
  }

  def index() = Authenticated { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

}
