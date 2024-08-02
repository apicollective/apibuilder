package services

import db.{DbUtils, InternalEmailVerificationConfirmationsDao, Helpers}
import helpers.ValidatedTestHelpers
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EmailVerificationsServiceSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers with ValidatedTestHelpers with DbUtils {

  private def emailVerificationConfirmationsDao: InternalEmailVerificationConfirmationsDao = injector.instanceOf[InternalEmailVerificationConfirmationsDao]
  private def service: EmailVerificationsService = injector.instanceOf[EmailVerificationsService]

  "isExpired" must {
    val user = createRandomUser()

    "expired" in {
      val verification = emailVerificationsDao.upsert(testUser, user, user.email)
      execute(
        Query("update email_verifications set expires_at = now() - interval '1 month'")
          .equals("guid", verification.guid)
      )
      val v2 = emailVerificationsDao.findByGuid(verification.guid).get

      expectInvalid {
        service.confirm(None, v2)
      } mustBe Seq(s"Token for verificationGuid[${v2.guid}] is expired")
    }

    "not expired" in {
      val verification = emailVerificationsDao.upsert(testUser, user, user.email)

      expectValid {
        service.confirm(None, verification)
      }
    }
  }

  "confirm" in {
    val user = createRandomUser()
    val verification = emailVerificationsDao.upsert(testUser, user, user.email)
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid), limit = None) must be(Nil)

    service.confirm(None, verification)
    emailVerificationConfirmationsDao.findAll(emailVerificationGuid = Some(verification.guid), limit = None).map(_.emailVerificationGuid) must be(Seq(verification.guid))
  }

}
