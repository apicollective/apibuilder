package db

import io.apibuilder.api.v0.models.UserForm
import lib.Role
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class EmailVerificationsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  def emailVerificationConfirmationsDao = play.api.Play.current.injector.instanceOf[db.EmailVerificationConfirmationsDao]
  it("upsert") {
    val user = Util.createRandomUser()

    // Let actor create the email verification
    // TODO: Change to eventually
    Thread.sleep(1500)
    val verification1 = emailVerificationsDao.upsert(Util.createdBy, user, user.email)
    val verification2 = emailVerificationsDao.upsert(Util.createdBy, user, user.email)
    verification2.guid should be(verification1.guid)

    emailVerificationsDao.softDelete(Util.createdBy, verification1)
    val verification3 = emailVerificationsDao.upsert(Util.createdBy, user, user.email)
    verification3.guid should not be(verification1.guid)

    val verificationWithDifferentEmail = emailVerificationsDao.upsert(Util.createdBy, user, "other-" + user.email)
    verificationWithDifferentEmail.guid should not be(verification3.guid)
  }
  /*
  it("create") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)
    verification.userGuid should be(user.guid)
    verification.email should be(user.email)
  }

  it("isExpired") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)
    emailVerificationsDao.isExpired(verification) should be(false)
  }

  it("confirm") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid)) should be(Seq.empty)

    emailVerificationsDao.confirm(None, verification)
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid)).map(_.emailVerificationGuid) should be(Seq(verification.guid))
  }

  it("findByGuid") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)

    emailVerificationsDao.findByGuid(verification.guid).map(_.userGuid) should be(Some(user.guid))
    emailVerificationsDao.findByGuid(UUID.randomUUID) should be(None)
  }

  it("findByToken") {
    val user = Util.createRandomUser()
    val verification = emailVerificationsDao.create(Util.createdBy, user, user.email)

    emailVerificationsDao.findByToken(verification.token).map(_.userGuid) should be(Some(user.guid))
    emailVerificationsDao.findByToken(UUID.randomUUID.toString) should be(None)
  }

  it("findAll") {
    val user1 = Util.createRandomUser()
    val verification1 = emailVerificationsDao.create(Util.createdBy, user1, user1.email)

    val user2 = Util.createRandomUser()
    val verification2 = emailVerificationsDao.create(Util.createdBy, user2, user2.email)

    emailVerificationsDao.findAll(userGuid = Some(user1.guid)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(userGuid = Some(user2.guid)).map(_.userGuid).distinct should be(Seq(user2.guid))
    emailVerificationsDao.findAll(userGuid = Some(UUID.randomUUID)).map(_.userGuid).distinct should be(Seq.empty)

    emailVerificationsDao.findAll(isExpired = Some(false), userGuid = Some(user1.guid)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(isExpired = Some(true), userGuid = Some(user1.guid)).map(_.userGuid).distinct should be(Seq.empty)

    emailVerificationsDao.findAll(email = Some(user1.email)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(email = Some(user1.email.toUpperCase)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(email = Some(user2.email)).map(_.userGuid).distinct should be(Seq(user2.guid))
    emailVerificationsDao.findAll(email = Some(UUID.randomUUID.toString)).map(_.userGuid).distinct should be(Seq.empty)

    emailVerificationsDao.findAll(guid = Some(verification1.guid)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(guid = Some(verification2.guid)).map(_.userGuid).distinct should be(Seq(user2.guid))
    emailVerificationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid).distinct should be(Seq.empty)

    emailVerificationsDao.findAll(token = Some(verification1.token)).map(_.userGuid).distinct should be(Seq(user1.guid))
    emailVerificationsDao.findAll(token = Some(verification2.token)).map(_.userGuid).distinct should be(Seq(user2.guid))
    emailVerificationsDao.findAll(token = Some("bad")).map(_.userGuid).distinct should be(Seq.empty)
  }

  describe("membership requests") {

    it("confirm auto approves pending membership requests based on org email domain") {
      val org = Util.createOrganization()
      val domain = UUID.randomUUID.toString + ".com"

      organizationDomainsDao.create(Util.createdBy, org, domain)

      val prefix = "test-user-" + UUID.randomUUID.toString

      val user = usersDao.create(UserForm(
        email = prefix + "@" + domain,
        password = "testing"
      ))

      val nonMatchingUser = usersDao.create(UserForm(
        email = prefix + "@other." + domain,
        password = "testing"
      ))

      usersDao.processUserCreated(user.guid)
      usersDao.processUserCreated(nonMatchingUser.guid)

      membershipsDao.isUserMember(user, org) should be(false)
      membershipsDao.isUserMember(nonMatchingUser, org) should be(false)

      val verification = emailVerificationsDao.upsert(Util.createdBy, user, user.email)
      emailVerificationsDao.confirm(Some(Util.createdBy), verification)

      membershipsDao.isUserMember(user, org) should be(true)
      membershipsDao.isUserMember(nonMatchingUser, org) should be(false)
    }

  }
*/
}
