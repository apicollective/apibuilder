package controllers

import javax.inject.Inject
import io.apibuilder.api.v0.models.EmailVerificationConfirmationForm
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

class EmailVerifications @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def get(token: String): Action[AnyContent] = Anonymous.async { implicit request =>
    request.api.emailVerificationConfirmationForms.post(
      EmailVerificationConfirmationForm(token = token)
    ).map { _ =>
      Redirect(routes.ApplicationController.index()).flashing("success" -> "Email confirmed")
    }.recover {
      case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
        Redirect(routes.ApplicationController.index()).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
