package controllers

import com.bryzek.apidoc.api.v0.models.EmailVerificationConfirmationForm
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.EmailVerificationsDao
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class EmailVerificationConfirmationForms @Inject() (
  emailVerificationsDao: EmailVerificationsDao
) extends Controller {

  def post() = AnonymousRequest(parse.json) { request =>
    request.body.validate[EmailVerificationConfirmationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[EmailVerificationConfirmationForm] => {
        val token = s.get.token
        emailVerificationsDao.findByToken(token) match {
          case None => Conflict(Json.toJson("Token not found or has already expired"))
          case Some(verification) => {
            if (emailVerificationsDao.isExpired(verification)) {
              Conflict(Json.toJson("Token is expired"))
            } else {
              emailVerificationsDao.confirm(request.user, verification)
              NoContent
            }
          }
        }
      }
    }
  }

}
