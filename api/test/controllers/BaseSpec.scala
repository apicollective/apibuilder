package controllers

import com.gilt.apidoc.models._
import db._
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

abstract class BaseSpec extends PlaySpec with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val TestUser = UserDao.create(
    UserForm(
      email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
      password = UUID.randomUUID.toString
    )
  )

  lazy val apiToken = TokenDao.create(TestUser, TokenForm(userGuid = TestUser.guid)).token

  lazy val client = new com.gilt.apidoc.Client(s"http://localhost:$port", Some(apiToken)) {
    override def _requestHolder(path: String) = {
      super._requestHolder(path).withHeaders("X-User-Guid" -> TestUser.guid.toString)
    }
  }

  def createOrganization(
    form: OrganizationForm = createOrganizationForm()
  ): Organization = {
    await(client.organizations.post(form))
  }

  def createOrganizationForm() = OrganizationForm(
    name = "z-test-org-" + UUID.randomUUID.toString
  )

}
