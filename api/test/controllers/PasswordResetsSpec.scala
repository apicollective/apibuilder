package controllers

import db.PasswordResetRequestsDao
import io.apibuilder.api.v0.models.{Authentication, PasswordReset, PasswordResetRequest, User}
import io.apibuilder.api.v0.errors.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class PasswordResetsSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  def resetPassword(token: String, pwd: String): Authentication = {
    await(
      client.passwordResets.post(
        PasswordReset(token = token, password = pwd)
      )
    )
  }

  "POST /password_resets" in new WithServer(port=defaultPort) {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    val pwd = "some password"
    userPasswordsDao.isValid(user.guid, pwd) must beEqualTo(false)

    val result = resetPassword(pr.token, pwd)
    result.user.guid must beEqualTo(user.guid)

    userPasswordsDao.isValid(user.guid, pwd) must beEqualTo(true)

    // Make sure token cannot be reused
    intercept[ErrorsResponse] {
      resetPassword(pr.token, pwd)
    }.errors.map(_.message) must beEqualTo(Seq(s"Token not found"))

  }

  "POST /password_resets validates password" in new WithServer(port=defaultPort) {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    intercept[ErrorsResponse] {
      resetPassword(pr.token, "foo")
    }.errors.map(_.message) must beEqualTo(Seq(s"Password must be at least 5 characters"))

  }

  "POST /password_reset_requests/:token validates token" in new WithServer(port=defaultPort) {
    intercept[ErrorsResponse] {
      resetPassword(UUID.randomUUID.toString, "testing")
    }.errors.map(_.message) must beEqualTo(Seq(s"Token not found"))

  }

}
