package controllers

import com.gilt.apidoc.models.EmailVerificationConfirmationForm
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{EmailVerificationsDao, EmailVerificationConfirmationsDao}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object EmailVerificationConfirmationForms extends Controller {

  def post() = AnonymousRequest(parse.json) { request =>
    request.body.validate[EmailVerificationConfirmationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[EmailVerificationConfirmationForm] => {
        val token = s.get.token
        EmailVerificationsDao.findByToken(token) match {
          case None => Conflict(Json.toJson("Token not found or has already expired"))
          case Some(verification) => {
            if (EmailVerificationsDao.isExpired(verification)) {
              Conflict(Json.toJson("Token is expired"))
            } else {
              EmailVerificationsDao.confirm(request.user, verification)
              NoContent
            }
          }
        }
      }
    }
  }

}
