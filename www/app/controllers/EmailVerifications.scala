package controllers

import com.gilt.apidoc.v0.models.EmailVerificationConfirmationForm
import play.api._
import play.api.mvc._

object EmailVerifications extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def get(token: String) = Anonymous.async { implicit request =>
    request.api.emailVerificationConfirmationForms.postEmailVerificationConfirmations(
      EmailVerificationConfirmationForm(token = token)
    ).map { _ =>
      Redirect(routes.ApplicationController.index()).flashing("success" -> "Email confirmed")
    }.recover {
      case r: com.gilt.apidoc.v0.errors.ErrorsResponse => {
        Redirect(routes.ApplicationController.index()).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
