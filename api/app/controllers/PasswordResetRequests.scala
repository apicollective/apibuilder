package controllers

import com.bryzek.apidoc.api.v0.models.PasswordResetRequest
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.{PasswordResetRequestsDao, UsersDao}
import play.api.mvc._
import play.api.libs.json._

object PasswordResetRequests extends Controller {

  def post() = AnonymousRequest(parse.json) { request =>
    request.body.validate[PasswordResetRequest] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordResetRequest] => {
        UsersDao.findByEmail(s.get.email).map { user =>
          PasswordResetRequestsDao.create(request.user, user)
        }
        NoContent
      }
    }
  }

}
