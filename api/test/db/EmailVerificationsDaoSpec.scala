package db

import helpers.ValidatedTestHelpers
import io.apibuilder.api.v0.models.UserForm
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import processor.UserCreatedProcessor
import services.EmailVerificationsService

import java.util.UUID

class EmailVerificationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers with ValidatedTestHelpers {

  private def userCreatedProcessor: UserCreatedProcessor = injector.instanceOf[UserCreatedProcessor]
  private def service: EmailVerificationsService = injector.instanceOf[EmailVerificationsService]

  "upsert" in {
    val user = createRandomUser()

    // Let actor create the email verification
    // TODO: Change to eventually
    Thread.sleep(1500)
    val verification1 = emailVerificationsDao.upsert(testUser, user, user.email)
    val verification2 = emailVerificationsDao.upsert(testUser, user, user.email)
    verification2.guid must be(verification1.guid)

    emailVerificationsDao.softDelete(testUser, verification1)
    val verification3 = emailVerificationsDao.upsert(testUser, user, user.email)
    verification3.guid != verification1.guid must be(true)

    val verificationWithDifferentEmail = emailVerificationsDao.upsert(testUser, user, "other-" + user.email)
    verificationWithDifferentEmail.guid != verification3.guid must be(true)
  }

  "create" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(testUser, user, user.email)
    verification.userGuid must be(user.guid)
    verification.email must be(user.email)
  }

  "findByGuid" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(testUser, user, user.email)

    emailVerificationsDao.findByGuid(verification.guid).map(_.userGuid) must be(Some(user.guid))
    emailVerificationsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByToken" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.create(testUser, user, user.email)

    emailVerificationsDao.findByToken(verification.token).map(_.userGuid) must be(Some(user.guid))
    emailVerificationsDao.findByToken(UUID.randomUUID.toString) must be(None)
  }

  "findAll" in {
    val user1 = createRandomUser()
    val verification1 = emailVerificationsDao.create(testUser, user1, user1.email)

    val user2 = createRandomUser()
    val verification2 = emailVerificationsDao.create(testUser, user2, user2.email)

    emailVerificationsDao.findAll(userGuid = Some(user1.guid)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(userGuid = Some(user2.guid)).map(_.userGuid).distinct must be(Seq(user2.guid))
    emailVerificationsDao.findAll(userGuid = Some(UUID.randomUUID)).map(_.userGuid).distinct must be(Nil)

    emailVerificationsDao.findAll(isExpired = Some(false), userGuid = Some(user1.guid)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(isExpired = Some(true), userGuid = Some(user1.guid)).map(_.userGuid).distinct must be(Nil)

    emailVerificationsDao.findAll(email = Some(user1.email)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(email = Some(user1.email.toUpperCase)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(email = Some(user2.email)).map(_.userGuid).distinct must be(Seq(user2.guid))
    emailVerificationsDao.findAll(email = Some(UUID.randomUUID.toString)).map(_.userGuid).distinct must be(Nil)

    emailVerificationsDao.findAll(guid = Some(verification1.guid)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(guid = Some(verification2.guid)).map(_.userGuid).distinct must be(Seq(user2.guid))
    emailVerificationsDao.findAll(guid = Some(UUID.randomUUID)).map(_.userGuid).distinct must be(Nil)

    emailVerificationsDao.findAll(token = Some(verification1.token)).map(_.userGuid).distinct must be(Seq(user1.guid))
    emailVerificationsDao.findAll(token = Some(verification2.token)).map(_.userGuid).distinct must be(Seq(user2.guid))
    emailVerificationsDao.findAll(token = Some("bad")).map(_.userGuid).distinct must be(Nil)
  }

  "membership requests confirm auto approves pending membership requests based on org email domain" in {
    val org = createOrganization()
    val domain = UUID.randomUUID.toString + ".com"

    organizationDomainsDao.create(testUser, org, domain)

    val prefix = "test-user-" + UUID.randomUUID.toString

    val user = usersDao.create(UserForm(
      email = prefix + "@" + domain,
      password = "testing"
    ))

    val nonMatchingUser = usersDao.create(UserForm(
      email = prefix + "@other." + domain,
      password = "testing"
    ))

    userCreatedProcessor.processRecord(user.guid)
    userCreatedProcessor.processRecord(nonMatchingUser.guid)

    membershipsDao.isUserMember(user, org) must be(false)
    membershipsDao.isUserMember(nonMatchingUser, org) must be(false)

    val verification = emailVerificationsDao.upsert(testUser, user, user.email)
    expectValid {
      service.confirm(Some(testUser), verification)
    }

    membershipsDao.isUserMember(user, org) must be(true)
    membershipsDao.isUserMember(nonMatchingUser, org) must be(false)
  }

}
