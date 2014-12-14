package db

import com.gilt.apidoc.models.{OrganizationForm, OrganizationMetadataForm, Visibility}
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

  it("find by token") {
    val user = Util.createRandomUser()
    val verification = EmailVerificationsDao.create(Util.createdBy, user, user.email)

    EmailVerificationsDao.findAll(token = Some(verification.token)).map(_.userGuid) should be(Seq(user.guid))
  }


}
