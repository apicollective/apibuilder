package controllers

import db.PasswordResetRequestsDao
import io.apibuilder.api.v0.models.{PasswordReset, PasswordResetRequest, User}
import io.apibuilder.api.v0.errors.ErrorsResponse
import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class PasswordResetRequestsSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /password_reset_requests" in {
    val user = createUser()
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must equal(Nil)
    val pr = createPasswordRequest(user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must equal(Seq(user.guid))
  }

  "POST /password_reset_requests does not reveal whether or not email exists" in {
    val user = createUser()
    val pr = createPasswordRequest("other-" + user.email)
    passwordResetRequestsDao.findAll(userGuid = Some(user.guid)).map(_.userGuid) must equal(Nil)
  }

}
