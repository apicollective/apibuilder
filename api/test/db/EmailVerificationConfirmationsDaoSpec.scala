package db

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class EmailVerificationConfirmationsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  def emailVerificationConfirmationsDao = play.api.Play.current.injector.instanceOf[db.EmailVerificationConfirmationsDao]

  it("upsert") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)

    val conf = emailVerificationConfirmationsDao.upsert(Util.createdBy, verification)
    conf.emailVerificationGuid should be(verification.guid)

    val conf2 = emailVerificationConfirmationsDao.upsert(Util.createdBy, verification)
    conf2.guid should be(conf.guid)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val verification1 = emailVerificationsDao.create(Util.createdBy, user1, user1.email)
    val conf1 = emailVerificationConfirmationsDao.upsert(Util.createdBy, verification1)

    val user2 = Util.createRandomUser()
    val verification2 = emailVerificationsDao.create(Util.createdBy, user2, user2.email)
    val conf2 = emailVerificationConfirmationsDao.upsert(Util.createdBy, verification2)

    emailVerificationConfirmationsDao.findAll(guid = Some(conf1.guid)).map(_.guid) should be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(conf2.guid)).map(_.guid) should be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.guid) should be(Seq.empty)

    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification1.guid)).map(_.guid) should be(Seq(conf1.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification2.guid)).map(_.guid) should be(Seq(conf2.guid))
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(UUID.randomUUID)).map(_.guid) should be(Seq.empty)
  }

}
