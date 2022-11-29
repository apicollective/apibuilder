package controllers

import io.apibuilder.api.v0.models.PasswordResetRequest
import io.apibuilder.api.v0.models.json._
import lib.Validation
import db.{PasswordResetRequestsDao, UsersDao}
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class PasswordResetRequests @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  passwordResetRequestsDao: PasswordResetRequestsDao,
  usersDao: UsersDao
) extends ApiBuilderController {

  def post() = Anonymous(parse.json) { request =>
    request.body.validate[PasswordResetRequest] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[PasswordResetRequest] => {
        usersDao.findByEmail(s.get.email).map { user =>
          passwordResetRequestsDao.create(request.user, user)
        }
        NoContent
      }
    }
  }

}
