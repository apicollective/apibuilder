package controllers

import com.gilt.apidoc.api.v0.models._
import db.{Authorization, TokensDao, UsersDao}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

abstract class BaseSpec extends PlaySpec with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val TestUser = UsersDao.create(createUserForm())

  lazy val apiToken = {
    val token = TokensDao.create(TestUser, TokenForm(userGuid = TestUser.guid))
    TokensDao.findCleartextByGuid(Authorization.All, token.guid).get.token
  }

  lazy val apiAuth = com.gilt.apidoc.api.v0.Authorization.Basic(apiToken)

  lazy val client = newClient(TestUser)

  def newClient(user: User) = {
    new com.gilt.apidoc.api.v0.Client(s"http://localhost:$port", Some(apiAuth)) {
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

  def createOrganizationForm(
    name: String = "z-test-org-" + UUID.randomUUID.toString,
    key: Option[String] = None,
    namespace: String = "test." + UUID.randomUUID.toString
  ) = OrganizationForm(
    name = name,
    key = key,
    namespace = namespace
  )

  def createUser(): User = {
    val form = createUserForm()
    await(client.users.post(form))
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

  def createApplication(
    org: Organization,
    form: ApplicationForm = createApplicationForm()
  ): Application = {
    await(client.applications.postByOrgKey(org.key, form))
  }

  def createApplicationForm(
    name: String = "Test " + UUID.randomUUID.toString,
    key: Option[String] = None,
    description: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ) = db.Util.createApplicationForm(
    name = name,
    key = key,
    description = description,
    visibility = visibility
  )

  def createVersion(
    application: Application = createApplication(createOrganization()),
    form: VersionForm = createVersionForm(),
    version: String = "0.0.1"
  ): Version = {
    await(
      client.versions.postByOrgKeyAndVersion(
        orgKey = application.organization.key,
        version = version,
        versionForm = form
      )
    )
  }

  def createVersionForm(
    data: String = s"""{ "name": "test-${UUID.randomUUID}", "apidoc": { "version": "${com.gilt.apidoc.api.v0.Constants.Version}" } }"""
  ): VersionForm = {
    com.gilt.apidoc.api.v0.models.VersionForm(
      originalForm = OriginalForm(
        data = data
      )
    )
  }

  def createPasswordRequest(email: String) {
    await(client.passwordResetRequests.post(PasswordResetRequest(email = email)))
  }

}
