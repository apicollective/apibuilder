package db

import lib.Role
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class EmailVerificationsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("create") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)
    verification.userGuid should be(user.guid)
    verification.email should be(user.email)
  }

  it("upsert") {
    val user = Util.createRandomUser()
    val verification1 = EmailVerificationsDao.upsert(Util.createdBy, user, user.email)
    val verification2 = EmailVerificationsDao.upsert(Util.createdBy, user, user.email)
    verification2.guid should be(verification1.guid)

    EmailVerificationsDao.softDelete(Util.createdBy, verification1)
    val verification3 = EmailVerificationsDao.upsert(Util.createdBy, user, user.email)
    verification3.guid should not be(verification1.guid)

    val verificationWithDifferentEmail = EmailVerificationsDao.upsert(Util.createdBy, user, "other-" + user.email)
    verificationWithDifferentEmail.guid should not be(verification3.guid)
  }

  it("isExpired") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)
    EmailVerificationsDao.isExpired(verification) should be(false)
  }

  it("confirm") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)
    EmailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid)) should be(Seq.empty)

    EmailVerificationsDao.confirm(None, verification)
    EmailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid)).map(_.emailVerificationGuid) should be(Seq(verification.guid))
  }

  it("findByGuid") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)

    EmailVerificationsDao.findByGuid(verification.guid).map(_.userGuid) should be(Some(user.guid))
    EmailVerificationsDao.findByGuid(UUID.randomUUID) should be(None)
  }

  it("findByToken") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)

    EmailVerificationsDao.findByToken(verification.token).map(_.userGuid) should be(Some(user.guid))
    EmailVerificationsDao.findByToken(UUID.randomUUID.toString) should be(None)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val verification1 = EmailVerificationsDao.create(Util.createdBy, user1, user1.email)

    val user2 = Util.createRandomUser()
    val verification2 = EmailVerificationsDao.create(Util.createdBy, user2, user2.email)

    EmailVerificationsDao.findAll(userGuid = Some(user1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(userGuid = Some(user2.guid)).map(_.userGuid) should be(Seq(user2.guid))
    EmailVerificationsDao.findAll(userGuid = Some(UUID.randomUUID)).map(_.userGuid) should be(Seq.empty)

    EmailVerificationsDao.findAll(isExpired = Some(false), userGuid = Some(user1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(isExpired = Some(true), userGuid = Some(user1.guid)).map(_.userGuid) should be(Seq.empty)

    EmailVerificationsDao.findAll(email = Some(user1.email)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(email = Some(user1.email.toUpperCase)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(email = Some(user2.email)).map(_.userGuid) should be(Seq(user2.guid))
    EmailVerificationsDao.findAll(email = Some(UUID.randomUUID.toString)).map(_.userGuid) should be(Seq.empty)

    EmailVerificationsDao.findAll(guid = Some(verification1.guid)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(guid = Some(verification2.guid)).map(_.userGuid) should be(Seq(user2.guid))
    EmailVerificationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid) should be(Seq.empty)

    EmailVerificationsDao.findAll(token = Some(verification1.token)).map(_.userGuid) should be(Seq(user1.guid))
    EmailVerificationsDao.findAll(token = Some(verification2.token)).map(_.userGuid) should be(Seq(user2.guid))
    EmailVerificationsDao.findAll(token = Some("bad")).map(_.userGuid) should be(Seq.empty)
  }

  describe("membership requests") {

    it("confirm auto approves pending membership requests based on org email domain") {
      val org = Util.createOrganization()
      val domain = UUID.randomUUID.toString + ".com"

      OrganizationDomainsDao.create(Util.createdBy, org, domain)

      val user = UsersDao.create(UserForm(
        email = "person@" + domain,
        password = "testing"
      ))

      val nonMatchingUser = UsersDao.create(UserForm(
        email = "person@other." + domain,
        password = "testing"
      ))

      actors.UserActor.userCreated(user.guid)
      actors.UserActor.userCreated(nonMatchingUser.guid)

      MembershipsDao.isUserMember(user, org) should be(false)
      MembershipsDao.isUserMember(nonMatchingUser, org) should be(false)

      val verification = EmailVerificationsDao.upsert(Util.createdBy, user, user.email)
      EmailVerificationsDao.confirm(Some(Util.createdBy), verification)

      MembershipsDao.isUserMember(user, org) should be(true)
      MembershipsDao.isUserMember(nonMatchingUser, org) should be(false)
    }

  }

}
