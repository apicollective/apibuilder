package controllers

import cats.data.Validated.{Invalid, Valid}
import db.{InternalUserPasswordsDao, InternalUsersDao, InternalPasswordResetsDao}
import io.apibuilder.api.v0.models.PasswordReset
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import play.api.mvc.*
import util.SessionHelper
import play.api.libs.json.*

import javax.inject.{Inject, Singleton}

@Singleton
class PasswordResets @Inject() (
                                 val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                 passwordResetRequestsDao: InternalPasswordResetsDao,
                                 sessionHelper: SessionHelper,
                                 usersDao: InternalUsersDao,
                                 userPasswordsDao: InternalUserPasswordsDao,
) extends ApiBuilderController {

  def post(): Action[JsValue] = Anonymous(parse.json) { request =>
    request.body.validate[PasswordReset] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordReset] => {
        val form = s.get
        passwordResetRequestsDao.findByToken(form.token) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Token not found")))
          }

          case Some(pr) => {
            passwordResetRequestsDao.resetPassword(request.user, pr, form.password) match {
              case Valid(user) => Ok(Json.toJson(sessionHelper.createAuthentication(user)))
              case Invalid(errors) => Conflict(Json.toJson(errors.toNonEmptyList.toList))
            }
          }
        }
      }
    }
  }

}
