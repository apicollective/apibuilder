package db

import helpers.ValidatedTestHelpers

import java.util.UUID
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.EmailVerificationsService

class InternalPasswordResetsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers with ValidatedTestHelpers {

  private val service = app.injector.instanceOf[EmailVerificationsService]

  "create" in {
    val user = createRandomUser()
    val pr = passwordResetsDao.create(Some(testUser), user)
    pr.userGuid must be(user.guid)
  }

  "isExpired" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(testUser, user, user.email)
    expectValid {
      service.confirm(None, verification)
    }
  }

  "resetPassword" in {
    val user = createRandomUser()
    val pr = passwordResetsDao.create(Some(testUser), user)

    val newPassword = "testing"
    userPasswordsDao.isValid(user.guid, newPassword) must be(false)
    passwordResetsDao.resetPassword(None, pr, newPassword)
    userPasswordsDao.isValid(user.guid, newPassword) must be(true)
  }

  "findByGuid" in {
    val user = createRandomUser()
    val pr = passwordResetsDao.create(None, user)

    passwordResetsDao.findByGuid(pr.guid).map(_.userGuid) must be(Some(user.guid))
    passwordResetsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByToken" in {
    val user = createRandomUser()
    val pr = passwordResetsDao.create(None, user)

    passwordResetsDao.findByToken(pr.token).map(_.userGuid) must be(Some(user.guid))
    passwordResetsDao.findByToken(UUID.randomUUID.toString) must be(None)
  }

  "findAll" in {
    val user1 = createRandomUser()
    val pr1 = passwordResetsDao.create(None, user1)

    val user2 = createRandomUser()
    val pr2 = passwordResetsDao.create(None, user2)

    passwordResetsDao.findAll(isExpired = Some(false), guid = Some(pr1.guid), limit = None).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetsDao.findAll(isExpired = Some(true), guid = Some(pr1.guid), limit = None).map(_.userGuid) must be(Nil)

    passwordResetsDao.findAll(guid = Some(pr1.guid), limit = None).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetsDao.findAll(guid = Some(pr2.guid), limit = None).map(_.userGuid) must be(Seq(user2.guid))
    passwordResetsDao.findAll(guid = Some(UUID.randomUUID), limit = None).map(_.userGuid) must be(Nil)

    passwordResetsDao.findAll(token = Some(pr1.token), limit = None).map(_.userGuid) must be(Seq(user1.guid))
    passwordResetsDao.findAll(token = Some(pr2.token), limit = None).map(_.userGuid) must be(Seq(user2.guid))
    passwordResetsDao.findAll(token = Some("bad"), limit = None).map(_.userGuid) must be(Nil)
  }

}
