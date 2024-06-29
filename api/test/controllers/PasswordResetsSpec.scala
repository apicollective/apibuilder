package controllers

import io.apibuilder.api.v0.models.{Authentication, PasswordReset}
import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future

class PasswordResetsSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def resetPassword(token: String, pwd: String): Future[Authentication] = {
    client.passwordResets.post(
      PasswordReset(token = token, password = pwd)
    )
  }

  "POST /password_resets" in {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    val pwd = "some password"
    userPasswordsDao.isValid(user.guid, pwd) must equal(false)

    val result = await(resetPassword(pr.token, pwd))
    result.user.guid must equal(user.guid)

    userPasswordsDao.isValid(user.guid, pwd) must equal(true)

    // Make sure token cannot be reused
    expectErrors {
      resetPassword(pr.token, pwd)
    }.errors.map(_.message) must equal(Seq(s"Token not found"))

  }

  "POST /password_resets validates password" in {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    expectErrors {
      resetPassword(pr.token, "foo")
    }.errors.map(_.message) must equal(Seq(s"Password must be at least 5 characters"))

  }

  "POST /password_reset_requests/:token validates token" in {
    expectErrors {
      resetPassword(UUID.randomUUID.toString, "testing")
    }.errors.map(_.message) must equal(Seq(s"Token not found"))

  }

}
