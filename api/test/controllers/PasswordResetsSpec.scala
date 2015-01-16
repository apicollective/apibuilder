package controllers

import db.PasswordResetRequestsDao
import com.gilt.apidoc.v0.FailedRequest
import com.gilt.apidoc.v0.models.{PasswordReset, PasswordResetSuccess, PasswordResetRequest, User}
import com.gilt.apidoc.v0.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class PasswordResetsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def resetPassword(token: String, pwd: String): PasswordResetSuccess = {
    await(
      client.passwordResets.post(
        PasswordReset(token = token, password = pwd)
      )
    )
  }

  "POST /password_resets" in new WithServer {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    val pwd = "some password"
    db.UserPasswordsDao.isValid(user.guid, pwd) must be(false)

    val result = resetPassword(pr.token, pwd)
    result.userGuid must be(user.guid)

    db.UserPasswordsDao.isValid(user.guid, pwd) must be(true)

    // Make sure token cannot be reused
    intercept[ErrorsResponse] {
      resetPassword(pr.token, pwd)
    }.errors.map(_.message) must be(Seq(s"Token not found"))

  }

  "POST /password_resets validates password" in new WithServer {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    intercept[ErrorsResponse] {
      resetPassword(pr.token, "foo")
    }.errors.map(_.message) must be(Seq(s"Password must be at least 5 characters"))

  }

  "POST /password_reset_requests/:token validates token" in new WithServer {
    intercept[ErrorsResponse] {
      resetPassword(UUID.randomUUID.toString, "testing")
    }.errors.map(_.message) must be(Seq(s"Token not found"))

  }

}
