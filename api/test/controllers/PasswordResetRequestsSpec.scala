package controllers

import db.PasswordResetRequestsDao
import com.gilt.apidoc.FailedRequest
import com.gilt.apidoc.models.{PasswordReset, PasswordResetRequest, User}
import com.gilt.apidoc.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class PasswordResetRequestsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createPasswordRequest(email: String) {
    await(client.passwordResetRequests.post(PasswordResetRequest(email = email)))
  }

  def resetPassword(token: String, pwd: String) {
    await(
      client.passwordResetRequests.postByToken(
        token = token,
        passwordReset = PasswordReset(password = pwd)
      )
    )
  }

  "POST /password_reset_requests" in new WithServer {
    val user = createUser()
    PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq.empty)
    val pr = createPasswordRequest(user.email)
    PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq(user.guid))
  }

  "POST /password_reset_requests does not reveal whether or not email exists" in new WithServer {
    val user = createUser()
    val pr = createPasswordRequest("other-" + user.email)
    PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must be(Seq.empty)
  }

  "POST /password_reset_requests/:token" in new WithServer {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = PasswordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    val pwd = "some password"
    db.UserPasswordsDao.isValid(user.guid, pwd) must be(false)

    resetPassword(pr.token, pwd)

    db.UserPasswordsDao.isValid(user.guid, pwd) must be(true)

    // Make sure token cannot be reused
    intercept[ErrorsResponse] {
      resetPassword(pr.token, pwd)
    }.errors.map(_.message) must be(Seq(s"Token not found"))

  }

  "POST /password_reset_requests/:token validates password" in new WithServer {
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
