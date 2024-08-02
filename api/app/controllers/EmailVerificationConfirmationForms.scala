package controllers

import cats.data.Validated.{Invalid, Valid}
import db.EmailVerificationsDao
import io.apibuilder.api.v0.models.EmailVerificationConfirmationForm
import io.apibuilder.api.v0.models.json._
import lib.Validation
import play.api.libs.json._
import play.api.mvc._
import services.EmailVerificationsService

import javax.inject.{Inject, Singleton}

@Singleton
class EmailVerificationConfirmationForms @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  emailVerificationsDao: EmailVerificationsDao,
  service: EmailVerificationsService
) extends ApiBuilderController {

  def post(): Action[JsValue] = Anonymous(parse.json) { request =>
    request.body.validate[EmailVerificationConfirmationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: EmailVerificationConfirmationForm, _) => {
        val token = form.token
        emailVerificationsDao.findByToken(token) match {
          case None => Conflict(Json.toJson("Token not found or has already expired"))
          case Some(verification) => {
            service.confirm(request.user, verification) match {
              case Invalid(e) => {
                Conflict(Json.toJson(e.toNonEmptyList.toList.mkString(", ")))
              }
              case Valid(_) => {
                NoContent
              }
            }
          }
        }
      }
    }
  }

}
