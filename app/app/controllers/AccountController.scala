package controllers

import javax.inject.Inject

class AccountController @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents
) extends ApibuilderController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AccountController.index())
  }

  def index() = Identified { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

}
