package controllers

import com.bryzek.apidoc.api.v0.models.{PasswordReset, PasswordResetSuccess}
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.{PasswordResetRequestsDao, UserPasswordsDao, UsersDao}
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class PasswordResets @Inject() (
  passwordResetRequestsDao: PasswordResetRequestsDao,
  usersDao: UsersDao,
  userPasswordsDao: UserPasswordsDao
) {

  def post() = AnonymousRequest(parse.json) { request =>
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
                    case Nil => {
                      passwordResetRequestsDao.resetPassword(request.user, pr, form.password)
                      Ok(Json.toJson(PasswordResetSuccess(userGuid = user.guid)))
                    }
                    case errors => {
                      Conflict(Json.toJson(errors))
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
