package controllers

import io.apibuilder.api.v0.models.EmailVerificationConfirmationForm
import io.apibuilder.api.v0.models.json._
import db.EmailVerificationsDao
import javax.inject.{Inject, Singleton}
import lib.Validation
import play.api.mvc._
import play.api.libs.json._

@Singleton
class EmailVerificationConfirmationForms @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  emailVerificationsDao: EmailVerificationsDao
) extends ApiBuilderController {

  def post(): Action[JsValue] = Anonymous(parse.json) { request =>
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
