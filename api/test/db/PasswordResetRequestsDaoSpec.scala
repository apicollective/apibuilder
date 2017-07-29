package db

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class PasswordResetRequestsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  it("create") {
    val user = Util.createRandomUser()
    val pr = passwordResetRequestsDao.create(Some(Util.createdBy), user)
    pr.userGuid should be(user.guid)
  }

  it("isExpired") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)
    emailVerificationsDao.isExpired(verification) should be(false)
  }

  it("resetPassword") {
    val user = Util.createRandomUser()
    val pr = passwordResetRequestsDao.create(Some(Util.createdBy), user)

    val newPassword = "testing"
    userPasswordsDao.isValid(user.guid, newPassword) should be(false)
    passwordResetRequestsDao.resetPassword(None, pr, newPassword)
    userPasswordsDao.isValid(user.guid, newPassword) should be(true)
  }

  it("findByGuid") {
    val user = Util.createRandomUser()
    val pr = passwordResetRequestsDao.create(None, user)

    passwordResetRequestsDao.findByGuid(pr.guid).map(_.userGuid) should be(Some(user.guid))
    passwordResetRequestsDao.findByGuid(UUID.randomUUID) should be(None)
  }

  it("findByToken") {
    val user = Util.createRandomUser()
    val pr = passwordResetRequestsDao.create(None, user)

    passwordResetRequestsDao.findByToken(pr.token).map(_.userGuid) should be(Some(user.guid))
    passwordResetRequestsDao.findByToken(UUID.randomUUID.toString) should be(None)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val pr1 = passwordResetRequestsDao.create(None, user1)

    val user2 = Util.createRandomUser()
    val pr2 = passwordResetRequestsDao.create(None, user2)

    passwordResetRequestsDao.findAll(isExpired = Some(false), guid = Some(pr1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(isExpired = Some(true), guid = Some(pr1.guid)).map(_.userGuid) should be(Nil)

    passwordResetRequestsDao.findAll(guid = Some(pr1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(guid = Some(pr2.guid)).map(_.userGuid) should be(Seq(user2.guid))
    passwordResetRequestsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid) should be(Nil)

    passwordResetRequestsDao.findAll(token = Some(pr1.token)).map(_.userGuid) should be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(token = Some(pr2.token)).map(_.userGuid) should be(Seq(user2.guid))
    passwordResetRequestsDao.findAll(token = Some("bad")).map(_.userGuid) should be(Nil)
  }

}
