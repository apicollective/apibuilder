package controllers

import com.gilt.apidoc.models.{PasswordResetRequest, PasswordReset}
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{PasswordResetsDao, UsersDao, UserPasswordsDao}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object PasswordResetRequests extends Controller {

  def post() = AnonymousRequest(parse.json) { request =>
    request.body.validate[PasswordResetRequest] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordResetRequest] => {
        UsersDao.findByEmail(s.get.email).map { user =>
          global.Actors.mainActor ! actors.MainActor.Messages.PasswordResetCreated(user.guid)
          // TODO: Send email
        }
        NoContent
      }
    }
  }

  def postByToken(token: String) = AnonymousRequest(parse.json) { request =>
    request.body.validate[PasswordReset] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordReset] => {
        PasswordResetsDao.findByToken(s.get.token) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Token not found")))
          }

          case Some(pr) => {
            UserPasswordsDao.validate(s.get.password) match {
              case Nil => {
                PasswordResetsDao.resetPassword(request.user, pr, s.get.password)
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
