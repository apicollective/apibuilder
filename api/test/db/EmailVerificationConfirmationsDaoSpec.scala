package db

import java.util.UUID

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EmailVerificationConfirmationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private def emailVerificationConfirmationsDao: EmailVerificationConfirmationsDao = injector.instanceOf[db.EmailVerificationConfirmationsDao]

  "upsert" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(testUser, user, user.email)

    val conf = emailVerificationConfirmationsDao.upsert(testUser.guid, verification)
    conf.emailVerificationGuid must be(verification.guid)

    val conf2 = emailVerificationConfirmationsDao.upsert(testUser.guid, verification)
    conf2.guid must be(conf.guid)
  }

  "findAll" in {
    val user1 = createRandomUser()
    val verification1 = emailVerificationsDao.create(testUser, user1, user1.email)
    val conf1 = emailVerificationConfirmationsDao.upsert(testUser.guid, verification1)

    val user2 = createRandomUser()
    val verification2 = emailVerificationsDao.create(testUser, user2, user2.email)
    val conf2 = emailVerificationConfirmationsDao.upsert(testUser.guid, verification2)

    emailVerificationConfirmationsDao.findAll(guid = Some(conf1.guid)).map(_.guid) must be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(conf2.guid)).map(_.guid) must be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)

    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification1.guid)).map(_.guid) must be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification2.guid)).map(_.guid) must be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
  }

}
