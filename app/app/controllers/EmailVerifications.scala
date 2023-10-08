package controllers

import javax.inject.Inject

import io.apibuilder.api.v0.models.EmailVerificationConfirmationForm

class EmailVerifications @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def get(token: String) = Anonymous.async { implicit request =>
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
