package controllers

import io.apibuilder.api.v0.models.{Authentication, PasswordReset}
import java.util.UUID

import org.scalatestplus.play.OneServerPerSuite
import play.api.test._

import scala.concurrent.Future

class PasswordResetsSpec extends PlaySpecification with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] def resetPassword(token: String, pwd: String): Future[Authentication] = {
    client.passwordResets.post(
      PasswordReset(token = token, password = pwd)
    )
  }

  "POST /password_resets" in new WithServer(port=defaultPort) {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    val pwd = "some password"
    userPasswordsDao.isValid(user.guid, pwd) must beEqualTo(false)

    val result = await(resetPassword(pr.token, pwd))
    result.user.guid must beEqualTo(user.guid)

    userPasswordsDao.isValid(user.guid, pwd) must beEqualTo(true)

    // Make sure token cannot be reused
    expectErrors {
      resetPassword(pr.token, pwd)
    }.errors.map(_.message) must beEqualTo(Seq(s"Token not found"))

  }

  "POST /password_resets validates password" in new WithServer(port=defaultPort) {
    val user = createUser()
    createPasswordRequest(user.email)
    val pr = passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).head

    expectErrors {
      resetPassword(pr.token, "foo")
    }.errors.map(_.message) must beEqualTo(Seq(s"Password must be at least 5 characters"))

  }

  "POST /password_reset_requests/:token validates token" in new WithServer(port=defaultPort) {
    expectErrors {
      resetPassword(UUID.randomUUID.toString, "testing")
    }.errors.map(_.message) must beEqualTo(Seq(s"Token not found"))

  }

}
