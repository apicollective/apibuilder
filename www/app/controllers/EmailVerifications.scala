package controllers

import com.gilt.apidoc.models.EmailVerificationConfirmationForm
import play.api._
import play.api.mvc._

object EmailVerifications extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def get(token: String) = Anonymous.async { implicit request =>
    request.api.emailVerificationConfirmationForms.post(
      EmailVerificationConfirmationForm(token = token)
    ).map { _ =>
      Redirect(routes.Application.index()).flashing("success" -> "Email confirmed")
    }.recover {
      case r: com.gilt.apidoc.error.ErrorsResponse => {
        Redirect(routes.Application.index()).flashing("warning" -> r.errors.map(_.message).mkString(", "))
      }
    }
  }

}
