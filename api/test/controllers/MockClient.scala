package controllers

import java.util.UUID
import db.{Authorization, InternalApplication, InternalOrganization}
import io.apibuilder.api.v0.Client
import io.apibuilder.api.v0.errors.UnitResponse
import io.apibuilder.api.v0.models._
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait MockClient extends db.Helpers
  with FutureAwaits
  with DefaultAwaitTimeout
{

  import scala.concurrent.ExecutionContext.Implicits.global

  def port: Int

  def wsClient: WSClient = injector.instanceOf[WSClient]

  private val DefaultDuration = FiniteDuration(3, SECONDS)

  private lazy val apiToken = {
    val token = tokensDao.create(testUser, TokenForm(userGuid = testUser.guid))
    tokensDao.findCleartextByGuid(Authorization.All, token.guid).get.token
  }
  private lazy val apiAuth = io.apibuilder.api.v0.Authorization.Basic(apiToken)

  lazy val client: Client = newClient(testUser)

  def newClient(user: User): Client = {
    val auth = sessionHelper.createAuthentication(user)
    newSessionClient(auth.session.id)
  }

  def newSessionClient(sessionId: String): Client = {
    new io.apibuilder.api.v0.Client(
      wsClient,
      s"http://localhost:$port",
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
  ): Unit = {
    expectStatus(404) {
      Await.result(f, duration)
    }
  }

  def expectNotAuthorized[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ): Unit =  {
    expectStatus(401) {
      Await.result(f, duration)
    }
  }

  def expectStatus(code: Int)(f: => Unit): Unit = {
    Try(
      f
    ) match {
      case Success(_) => // no-op
      case Failure(ex) => ex match {
        case UnitResponse(c) if c == code => // no-op
        case UnitResponse(c) => sys.error(s"Expected code[$c] but got[$code]")
        case e => sys.error(s"Unexpected error: $e")
      }
    }
  }

  def createAttribute(
    form: AttributeForm = createAttributeForm()
  ): Attribute = {
    await(client.attributes.post(form))
  }

  def createSubscriptionForm(
    org: InternalOrganization = createOrganization(),
    user: User = createUser()
  ) = SubscriptionForm(
    organizationKey = org.key,
    userGuid = user.guid,
    publication = Publication.MembershipRequestsCreate
  )

  def createTokenForm(
    user: User = createUser()
  ): TokenForm = TokenForm(
    userGuid = user.guid,
    description = Some("test")
  )

  def createVersionThroughApi(
    application: InternalApplication = createApplication(createOrganization()),
    form: Option[VersionForm] = None,
    version: String = "0.0.1"
  ): Version = {
    val org = organizationsDao.findByGuid(Authorization.All, application.organizationGuid).get
    await(
      client.versions.putByApplicationKeyAndVersion(
        orgKey = org.key,
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

  def createPasswordRequest(email: String): Unit = {
    await(client.passwordResetRequests.post(PasswordResetRequest(email = email)))
  }

}
