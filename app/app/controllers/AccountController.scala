package controllers

import javax.inject.Inject

import play.api.mvc._

class AccountController @Inject() (
  val controllerComponents: ControllerComponents
) extends BaseController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AccountController.index())
  }

  def index() = Authenticated { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

}
