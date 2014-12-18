package controllers

import db.PasswordResetRequestsDao
import com.gilt.apidoc.FailedRequest
import com.gilt.apidoc.models.{User, PasswordResetRequest}
import com.gilt.apidoc.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class PasswordResetRequestsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createPasswordRequest(email: String) {
    await(client.passwordResetRequests.post(PasswordResetRequest(email = email)))
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

}
