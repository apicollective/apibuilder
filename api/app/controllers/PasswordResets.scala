package controllers

import com.gilt.apidoc.models.PasswordReset
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{PasswordResetRequestsDao, UserPasswordsDao}
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
            UserPasswordsDao.validate(form.password) match {
              case Nil => {
                PasswordResetRequestsDao.resetPassword(request.user, pr, form.password)
                NoContent
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
