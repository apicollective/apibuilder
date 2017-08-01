package controllers

import java.util.UUID

import db.Authorization
import io.apibuilder.api.v0.Client
import io.apibuilder.api.v0.errors.UnitResponse
import io.apibuilder.api.v0.models._
import play.api.test.Helpers._
import util.SessionHelper

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  val defaultPort: Int = 9010

  private[this] val DefaultDuration = FiniteDuration(3, SECONDS)
  private[this] def app = play.api.Play.current

  def applicationsDao = app.injector.instanceOf[db.ApplicationsDao]
  def attributesDao = app.injector.instanceOf[db.AttributesDao]
  def changesDao = app.injector.instanceOf[db.ChangesDao]
  def emailVerificationsDao = app.injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao = app.injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao = app.injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao = app.injector.instanceOf[db.MembershipsDao]
  def usersDao = app.injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao = app.injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao = app.injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao = app.injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao = app.injector.instanceOf[db.OrganizationsDao]
  def originalsDao = app.injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao = app.injector.instanceOf[db.PasswordResetRequestsDao]
  def subscriptionsDao = app.injector.instanceOf[db.SubscriptionsDao]
  def tasksDao = app.injector.instanceOf[db.TasksDao]
  def tokensDao = app.injector.instanceOf[db.TokensDao]
  def userPasswordsDao = app.injector.instanceOf[db.UserPasswordsDao]
  def versionsDao = app.injector.instanceOf[db.VersionsDao]

  def servicesDao = app.injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao = app.injector.instanceOf[db.generators.GeneratorsDao]

  def emails = app.injector.instanceOf[actors.Emails]
  def search = app.injector.instanceOf[actors.Search]

  def sessionHelper = app.injector.instanceOf[SessionHelper]

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
      s"http://localhost:$defaultPort",
      Some(apiAuth),
      defaultHeaders = Seq("Authorization" -> s"Session $sessionId")
    )
  }

  def expectErrors[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ): io.apibuilder.api.v0.errors.ErrorsResponse = {
    Try(
      Await.result(f, duration)
    ) match {
      case Success(response) => {
        sys.error("Expected function to fail but it succeeded with: " + response)
      }
      case Failure(ex) =>  ex match {
        case e: io.apibuilder.api.v0.errors.ErrorsResponse => {
          e
        }
        case e => {
          sys.error(s"Expected an exception of type[GenericErrorResponse] but got[$e]")
        }
      }
    }
  }

  def expectNotFound[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ) {
    expectStatus(404) {
      Await.result(f, duration)
    }
  }

  def expectNotAuthorized[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ) {
    expectStatus(401) {
      Await.result(f, duration)
    }
  }

  def expectStatus(code: Int)(f: => Unit) {
    Try(
      f
    ) match {
      case Success(response) => {
        org.specs2.execute.Failure(s"Expected HTTP[$code] but got HTTP 2xx")
      }
      case Failure(ex) => ex match {
        case UnitResponse(_) => {
          org.specs2.execute.Success()
        }
        case e => {
          org.specs2.execute.Failure(s"Unexpected error: $e")
        }
      }
    }
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
