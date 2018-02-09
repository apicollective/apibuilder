package db

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class PasswordResetRequestsDaoSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  "create" in {
    val user = createRandomUser()
    val pr = passwordResetRequestsDao.create(Some(createdBy), user)
    pr.userGuid must be(user.guid)
  }

  "isExpired" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(createdBy, user, user.email)
    emailVerificationsDao.isExpired(verification) must be(false)
  }

  "resetPassword" in {
    val user = createRandomUser()
    val pr = passwordResetRequestsDao.create(Some(createdBy), user)

    val newPassword = "testing"
    userPasswordsDao.isValid(user.guid, newPassword) must be(false)
    passwordResetRequestsDao.resetPassword(None, pr, newPassword)
    userPasswordsDao.isValid(user.guid, newPassword) must be(true)
  }

  "findByGuid" in {
    val user = createRandomUser()
    val pr = passwordResetRequestsDao.create(None, user)

    passwordResetRequestsDao.findByGuid(pr.guid).map(_.userGuid) must be(Some(user.guid))
    passwordResetRequestsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByToken" in {
    val user = createRandomUser()
    val pr = passwordResetRequestsDao.create(None, user)

    passwordResetRequestsDao.findByToken(pr.token).map(_.userGuid) must be(Some(user.guid))
    passwordResetRequestsDao.findByToken(UUID.randomUUID.toString) must be(None)
  }

  "findAll" in {
    val user1 = createRandomUser()
    val pr1 = passwordResetRequestsDao.create(None, user1)

    val user2 = createRandomUser()
    val pr2 = passwordResetRequestsDao.create(None, user2)

    passwordResetRequestsDao.findAll(isExpired = Some(false), guid = Some(pr1.guid)).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(isExpired = Some(true), guid = Some(pr1.guid)).map(_.userGuid) must be(Nil)

    passwordResetRequestsDao.findAll(guid = Some(pr1.guid)).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(guid = Some(pr2.guid)).map(_.userGuid) must be(Seq(user2.guid))
    passwordResetRequestsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid) must be(Nil)

    passwordResetRequestsDao.findAll(token = Some(pr1.token)).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetRequestsDao.findAll(token = Some(pr2.token)).map(_.userGuid) must be(Seq(user2.guid))
    passwordResetRequestsDao.findAll(token = Some("bad")).map(_.userGuid) must be(Nil)
  }

}
