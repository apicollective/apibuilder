package controllers

import db.PasswordResetRequestsDao
import io.apibuilder.api.v0.models.{PasswordReset, PasswordResetRequest, User}
import io.apibuilder.api.v0.errors.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class PasswordResetRequestsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /password_reset_requests" in new WithServer {
    val user = createUser()
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq.empty)
    val pr = createPasswordRequest(user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq(user.guid))
  }

  "POST /password_reset_requests does not reveal whether or not email exists" in new WithServer {
    val user = createUser()
    val pr = createPasswordRequest("other-" + user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq.empty)
  }

}
