package controllers

import java.util.UUID

import db.Authorization
import io.apibuilder.api.v0.Client
import io.apibuilder.api.v0.errors.UnitResponse
import io.apibuilder.api.v0.models._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait MockClient extends db.Helpers
  with FutureAwaits
  with DefaultAwaitTimeout
{

  import scala.concurrent.ExecutionContext.Implicits.global

  val defaultPort: Int = 9010

  private[this] val DefaultDuration = FiniteDuration(3, SECONDS)

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

  def createSubscriptionForm(
    org: Organization = createOrganization(),
    user: User = createUser()
  ) = SubscriptionForm(
    organizationKey = org.key,
    userGuid = user.guid,
    publication = Publication.MembershipRequestsCreate
  )

  def createTokenForm(
    user: User = createUser()
  ) = TokenForm(
    userGuid = user.guid,
    description = Some("test")
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
  ): ApplicationForm = db.createApplicationForm(
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
