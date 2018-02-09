package controllers

import db.PasswordResetRequestsDao
import io.apibuilder.api.v0.models.{PasswordReset, PasswordResetRequest, User}
import io.apibuilder.api.v0.errors.ErrorsResponse
import java.util.UUID

import org.scalatestplus.play.OneServerPerSuite
import play.api.test._

class PasswordResetRequestsSpec extends PlaySpecification with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /password_reset_requests" in new WithServer(port=defaultPort) {
    val user = createUser()
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must beEqualTo(Nil)
    val pr = createPasswordRequest(user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must beEqualTo(Seq(user.guid))
  }

  "POST /password_reset_requests does not reveal whether or not email exists" in new WithServer(port=defaultPort) {
    val user = createUser()
    val pr = createPasswordRequest("other-" + user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must beEqualTo(Nil)
  }

}
