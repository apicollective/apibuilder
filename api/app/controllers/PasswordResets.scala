package controllers

import com.gilt.apidoc.api.v0.models.{PasswordReset, PasswordResetSuccess}
import com.gilt.apidoc.api.v0.models.json._
import lib.Validation
import db.{PasswordResetRequestsDao, UserPasswordsDao, UsersDao}
import play.api.mvc._
import play.api.libs.json._

object PasswordResets extends Controller {

  def post() = AnonymousRequest(parse.json) { request =>
    request.body.validate[PasswordReset] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordReset] => {
        val form = s.get
        PasswordResetRequestsDao.findByToken(form.token) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Token not found")))
          }

          case Some(pr) => {
            if (PasswordResetRequestsDao.isExpired(pr)) {
              Conflict(Json.toJson(Validation.error("Token is expired")))
            } else {
              UsersDao.findByGuid(pr.userGuid) match {
                case None => {
                  Conflict(Json.toJson(Validation.error("User not found")))
                }

                case Some(user) => {
                  UserPasswordsDao.validate(form.password) match {
                    case Nil => {
                      PasswordResetRequestsDao.resetPassword(request.user, pr, form.password)
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
