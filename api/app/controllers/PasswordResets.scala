package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.PasswordReset
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import db.{InternalUsersDao, PasswordResetRequestsDao, UserPasswordsDao}
import models.UsersModel
import util.SessionHelper

import javax.inject.{Inject, Singleton}
import play.api.mvc.*
import play.api.libs.json.*

@Singleton
class PasswordResets @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  passwordResetRequestsDao: PasswordResetRequestsDao,
  sessionHelper: SessionHelper,
  usersDao: InternalUsersDao,
  userPasswordsDao: UserPasswordsDao,
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
            if (passwordResetRequestsDao.isExpired(pr)) {
              Conflict(Json.toJson(Validation.error("Token is expired")))
            } else {
              usersDao.findByGuid(pr.userGuid) match {
                case None => {
                  Conflict(Json.toJson(Validation.error("User not found")))
                }

                case Some(user) => {
                  userPasswordsDao.validate(form.password) match {
                    case Valid(_) => {
                      passwordResetRequestsDao.resetPassword(request.user, pr, form.password)
                      Ok(Json.toJson(sessionHelper.createAuthentication(user)))
                    }
                    case Invalid(errors) => {
                      Conflict(Json.toJson(errors.toNonEmptyList.toList))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}
