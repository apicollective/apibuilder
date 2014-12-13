package controllers

import com.gilt.apidoc.models._
import db.{TokensDao, UsersDao, UserForm}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

abstract class BaseSpec extends PlaySpec with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val TestUser = UsersDao.create(createUserForm())

  lazy val apiToken = TokensDao.create(TestUser, TokenForm(userGuid = TestUser.guid)).token

  lazy val client = newClient(TestUser)

  def newClient(user: User) = {
    new com.gilt.apidoc.Client(s"http://localhost:$port", Some(apiToken)) {
      override def _requestHolder(path: String) = {
        super._requestHolder(path).withHeaders("X-User-Guid" -> user.guid.toString)
      }
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

  def createUser(): User = {
    val form = createUserForm()
    await(
      client.users.post(
        email = form.email,
        password = form.password,
        name = form.name
      )
    )
  }

  def createUserForm() = UserForm(
    email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
    password = UUID.randomUUID.toString,
    name = None
  )

  def createSubscription(
    form: SubscriptionForm = createSubscriptionForm()
  ): Subscription = {
    await(client.subscriptions.post(form))
  }

  def createSubscriptionForm(
    org: Organization = createOrganization(),
    user: User = createUser()
  ) = SubscriptionForm(
    organizationKey = org.key,
    userGuid = user.guid,
    publication = Publication.MembershipRequestsCreate
  )

  def createService(
    org: Organization,
    key: String = "z-test-service-" + UUID.randomUUID.toString
  ): Service = {
    db.ServicesDao.create(
      createdBy = TestUser,
      org = org,
      form = db.ServiceForm(
        name = key,
        key = Some(key),
        description = None,
        visibility = Visibility.Organization
      )
    )
  }

}
