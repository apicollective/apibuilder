package db

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class EmailVerificationConfirmationsDaoSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  def emailVerificationConfirmationsDao = play.api.Play.current.injector.instanceOf[db.EmailVerificationConfirmationsDao]

  "upsert" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(createdBy, user, user.email)

    val conf = emailVerificationConfirmationsDao.upsert(createdBy.guid, verification)
    conf.emailVerificationGuid must be(verification.guid)

    val conf2 = emailVerificationConfirmationsDao.upsert(createdBy.guid, verification)
    conf2.guid must be(conf.guid)
  }

  "findAll" in {
    val user1 = createRandomUser()
    val verification1 = emailVerificationsDao.create(createdBy, user1, user1.email)
    val conf1 = emailVerificationConfirmationsDao.upsert(createdBy.guid, verification1)

    val user2 = createRandomUser()
    val verification2 = emailVerificationsDao.create(createdBy, user2, user2.email)
    val conf2 = emailVerificationConfirmationsDao.upsert(createdBy.guid, verification2)

    emailVerificationConfirmationsDao.findAll(guid = Some(conf1.guid)).map(_.guid) must be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(conf2.guid)).map(_.guid) must be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)

    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification1.guid)).map(_.guid) must be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification2.guid)).map(_.guid) must be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
  }

}
