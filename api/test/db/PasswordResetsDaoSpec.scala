package db

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class PasswordResetsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("create") {
    val user = Util.createRandomUser()
    val pr = PasswordResetsDao.create(Some(Util.createdBy), user)
    pr.userGuid should be(user.guid)
  }

  it("isExpired") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)
    EmailVerificationsDao.isExpired(verification) should be(false)
  }

  it("resetPassword") {
    val user = Util.createRandomUser()
    val pr = PasswordResetsDao.create(Some(Util.createdBy), user)

    val newPassword = "testing"
    UserPasswordsDao.isValid(user.guid, newPassword) should be(false)
    PasswordResetsDao.resetPassword(None, pr, newPassword)
    UserPasswordsDao.isValid(user.guid, newPassword) should be(true)
  }

  it("findByGuid") {
    val user = Util.createRandomUser()
    val pr = PasswordResetsDao.create(None, user)

    PasswordResetsDao.findByGuid(pr.guid).map(_.userGuid) should be(Some(user.guid))
    PasswordResetsDao.findByGuid(UUID.randomUUID) should be(None)
  }

  it("findByToken") {
    val user = Util.createRandomUser()
    val pr = PasswordResetsDao.create(None, user)

    PasswordResetsDao.findByToken(pr.token).map(_.userGuid) should be(Some(user.guid))
    PasswordResetsDao.findByToken(UUID.randomUUID.toString) should be(None)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val pr1 = PasswordResetsDao.create(None, user1)

    val user2 = Util.createRandomUser()
    val pr2 = PasswordResetsDao.create(None, user2)

    PasswordResetsDao.findAll(isExpired = Some(false), guid = Some(pr1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    PasswordResetsDao.findAll(isExpired = Some(true), guid = Some(pr1.guid)).map(_.userGuid) should be(Seq.empty)

    PasswordResetsDao.findAll(guid = Some(pr1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    PasswordResetsDao.findAll(guid = Some(pr2.guid)).map(_.userGuid) should be(Seq(user2.guid))
    PasswordResetsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid) should be(Seq.empty)

    PasswordResetsDao.findAll(token = Some(pr1.token)).map(_.userGuid) should be(Seq(user1.guid))
    PasswordResetsDao.findAll(token = Some(pr2.token)).map(_.userGuid) should be(Seq(user2.guid))
    PasswordResetsDao.findAll(token = Some("bad")).map(_.userGuid) should be(Seq.empty)
  }

}
