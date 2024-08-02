package controllers

import db.{InternalUsersDao, PasswordResetRequestsDao}
import io.apibuilder.api.v0.models.PasswordResetRequest
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import models.UsersModel
import play.api.libs.json.*
import play.api.mvc.*

import javax.inject.{Inject, Singleton}

@Singleton
class PasswordResetRequests @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  passwordResetRequestsDao: PasswordResetRequestsDao,
  usersDao: InternalUsersDao,
) extends ApiBuilderController {

  def post(): Action[JsValue] = Anonymous(parse.json) { request =>
    request.body.validate[PasswordResetRequest] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(data: PasswordResetRequest, _) => {
        usersDao.findByEmail(data.email).map { user =>
          passwordResetRequestsDao.create(request.user, user)
        }
        NoContent
      }
    }
  }

}
