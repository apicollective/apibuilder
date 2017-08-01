package controllers

import io.apibuilder.api.v0.models._
import db.Authorization
import java.util.UUID

import io.apibuilder.api.v0.Client
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import util.SessionHelper

class BaseSpec extends PlaySpec with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  def applicationsDao = play.api.Play.current.injector.instanceOf[db.ApplicationsDao]
  def attributesDao = play.api.Play.current.injector.instanceOf[db.AttributesDao]
  def changesDao = play.api.Play.current.injector.instanceOf[db.ChangesDao]
  def emailVerificationsDao = play.api.Play.current.injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao = play.api.Play.current.injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao = play.api.Play.current.injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao = play.api.Play.current.injector.instanceOf[db.MembershipsDao]
  def usersDao = play.api.Play.current.injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao = play.api.Play.current.injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao = play.api.Play.current.injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao = play.api.Play.current.injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao = play.api.Play.current.injector.instanceOf[db.OrganizationsDao]
  def originalsDao = play.api.Play.current.injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao = play.api.Play.current.injector.instanceOf[db.PasswordResetRequestsDao]
  def subscriptionsDao = play.api.Play.current.injector.instanceOf[db.SubscriptionsDao]
  def tasksDao = play.api.Play.current.injector.instanceOf[db.TasksDao]
  def tokensDao = play.api.Play.current.injector.instanceOf[db.TokensDao]
  def userPasswordsDao = play.api.Play.current.injector.instanceOf[db.UserPasswordsDao]
  def versionsDao = play.api.Play.current.injector.instanceOf[db.VersionsDao]

  def servicesDao = play.api.Play.current.injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao = play.api.Play.current.injector.instanceOf[db.generators.GeneratorsDao]

  def emails = play.api.Play.current.injector.instanceOf[actors.Emails]
  def search = play.api.Play.current.injector.instanceOf[actors.Search]

  def sessionHelper = play.api.Play.current.injector.instanceOf[SessionHelper]

  lazy val TestUser: User = usersDao.create(createUserForm())

  private[this] lazy val apiToken = {
    val token = tokensDao.create(TestUser, TokenForm(userGuid = TestUser.guid))
    tokensDao.findCleartextByGuid(Authorization.All, token.guid).get.token
  }
  private[this] lazy val apiAuth = io.apibuilder.api.v0.Authorization.Basic(apiToken)

  lazy val client: Client = newClient(TestUser)

  def newClient(user: User): Client = {
    val auth = sessionHelper.createAuthentication(user)
    newSessionClient(auth.session.id)
  }

  def newSessionClient(sessionId: String): Client = {
    new io.apibuilder.api.v0.Client(
      s"http://localhost:$port",
      Some(apiAuth),
      defaultHeaders = Seq("Authorization" -> s"Session $sessionId")
    )
  }

  def createRandomName(suffix: String): String = {
    s"z-test-$suffix-" + UUID.randomUUID.toString
  }
  
  def createOrganization(
    form: OrganizationForm = createOrganizationForm()
  ): Organization = {
    await(client.organizations.post(form))
  }

  def createOrganizationForm(
    name: String = createRandomName("org"),
    key: Option[String] = None,
    namespace: String = "test." + UUID.randomUUID.toString
  ) = OrganizationForm(
    name = name,
    key = key,
    namespace = namespace
  )

  def createAttribute(
    form: AttributeForm = createAttributeForm()
  ): Attribute = {
    await(client.attributes.post(form))
  }

  def createAttributeForm(
    name: String = createRandomName("attribute"),
    description: Option[String] = None
  ) = AttributeForm(
    name = name,
    description = description
  )
  
  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    await(client.users.post(form))
  }

  def createUserForm() = UserForm(
    email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
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
    await(client.applications.post(org.key, form))
  }

  def createApplicationForm(
    name: String = "Test " + UUID.randomUUID.toString,
    key: Option[String] = None,
    description: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ): ApplicationForm = db.Util.createApplicationForm(
    name = name,
    key = key,
    description = description,
    visibility = visibility
  )

  def createVersion(
    application: Application = createApplication(createOrganization()),
    form: Option[VersionForm] = None,
    version: String = "0.0.1"
  ): Version = {
    await(
      client.versions.putByApplicationKeyAndVersion(
        orgKey = application.organization.key,
        applicationKey = application.key,
        version = version,
        versionForm = form.getOrElse { createVersionForm(application.name) }
      )
    )
  }

  def createVersionForm(
    name: String = UUID.randomUUID.toString
  ): VersionForm = {
    val data = s"""{ "name": "$name" }"""
    io.apibuilder.api.v0.models.VersionForm(
      originalForm = OriginalForm(
        data = data
      )
    )
  }

  def createPasswordRequest(email: String) {
    await(client.passwordResetRequests.post(PasswordResetRequest(email = email)))
  }

}
