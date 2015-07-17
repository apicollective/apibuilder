package db

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class EmailVerificationConfirmationsDaoSpec extends FunSpec with Matchers {

  // new play.core.StaticApplication(new java.io.File("."))

  it("upsert") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)

    val conf = EmailVerificationConfirmationsDao.upsert(Util.createdBy, verification)
    conf.emailVerificationGuid should be(verification.guid)

    val conf2 = EmailVerificationConfirmationsDao.upsert(Util.createdBy, verification)
    conf2.guid should be(conf.guid)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val verification1 = EmailVerificationsDao.create(Util.createdBy, user1, user1.email)
    val conf1 = EmailVerificationConfirmationsDao.upsert(Util.createdBy, verification1)

    val user2 = Util.createRandomUser()
    val verification2 = EmailVerificationsDao.create(Util.createdBy, user2, user2.email)
    val conf2 = EmailVerificationConfirmationsDao.upsert(Util.createdBy, verification2)

    EmailVerificationConfirmationsDao.findAll(guid = Some(conf1.guid)).map(_.guid) should be(Seq(conf1.guid))
    EmailVerificationConfirmationsDao.findAll(guid = Some(conf2.guid)).map(_.guid) should be(Seq(conf2.guid))
    EmailVerificationConfirmationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.guid) should be(Seq.empty)

    EmailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification1.guid)).map(_.guid) should be(Seq(conf1.guid))
    EmailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification2.guid)).map(_.guid) should be(Seq(conf2.guid))
    EmailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(UUID.randomUUID)).map(_.guid) should be(Seq.empty)
  }

}
