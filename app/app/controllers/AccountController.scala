package controllers

import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AccountController @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.AccountController.index())
  }

  def index(): Action[AnyContent] = Identified { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

}
