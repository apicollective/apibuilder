package db

import io.apibuilder.api.v0.models.{Application, ApplicationForm, Organization, OrganizationForm, Original, OriginalType}
import io.apibuilder.api.v0.models.{Publication, Subscription, SubscriptionForm, User, UserForm, Version, Visibility}
import io.apibuilder.spec.v0.{models => spec}
import play.api.libs.json.{Json, JsObject}
import lib.Role
import java.util.UUID

object Util extends util.Daos {
  // new play.core.StaticApplication(new java.io.File("."))

  def createRandomUser(): User = {
    val email = "random-user-" + UUID.randomUUID.toString + "@test.apidoc.me"
    usersDao.create(UserForm(email = email, password = "test1"))
  }

  def upsertUser(
    email: String = "random-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
    name: String = "Admin",
    password: String = "test1"
  ): User = {
    usersDao.findByEmail(email).getOrElse {
      usersDao.create(UserForm(email = email, name = Some(name), password = password))
    }
  }

  def upsertOrganization(name: String): Organization = {
    organizationsDao.findAll(Authorization.All, name = Some(name)).headOption.getOrElse {
      createOrganization(name = Some(name))
    }
  }

  def createOrganization(
    createdBy: User = Util.createdBy,
    name: Option[String] = None,
    key: Option[String] = None,
    namespace: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ): Organization = {
    val form = createOrganizationForm(
      name = name.getOrElse("z-test-org-" + UUID.randomUUID.toString),
      key = key,
      namespace = namespace.getOrElse("test." + UUID.randomUUID.toString),
      visibility = visibility
    )
    organizationsDao.createWithAdministrator(createdBy, form)
  }

  def createOrganizationForm(
    name: String = "z-test-org-" + UUID.randomUUID.toString,
    key: Option[String] = None,
    namespace: String = "test." + UUID.randomUUID.toString,
    visibility: Visibility = Visibility.Organization,
    domains: Option[Seq[String]] = None
  ) = OrganizationForm(
    name = name,
    key = key,
    namespace = namespace,
    visibility = visibility,
    domains = domains
  )

  def createApplication(
    org: Organization = createOrganization(),
    form: ApplicationForm = createApplicationForm()
  ): Application = {
    applicationsDao.create(Util.createdBy, org, form)
  }

  def createApplicationForm(
    name: String = "z-test-app-" + UUID.randomUUID.toString,
    key: Option[String] = None,
    description: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ) = ApplicationForm(
    name = name,
    key = key,
    description = description,
    visibility = visibility
  )

  def createVersion(
    application: Application = createApplication(),
    version: String = "1.0.0",
    original: Original = createOriginal(),
    service: Option[spec.Service] = None
  ): Version = {
    versionsDao.create(
      Util.createdBy,
      application,
      version,
      original,
      service.getOrElse { Util.createService(application) }
    )
  }

  def createOriginal(): Original = {
    io.apibuilder.api.v0.models.Original(
      `type` = OriginalType.ApiJson,
      data = Json.obj(
        "apidoc" -> Json.obj(
          "version" -> io.apibuilder.spec.v0.Constants.Version
        ),
        "name" -> s"test-${UUID.randomUUID}"
      ).toString
    )
  }

  def createMembership(
    org: Organization,
    user: User = Util.createRandomUser(),
    role: Role = Role.Admin
  ): io.apibuilder.api.v0.models.Membership = {
    val request = membershipRequestsDao.upsert(Util.createdBy, org, user, role)
    membershipRequestsDao.accept(Util.createdBy, request)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, org, user, role).getOrElse {
      sys.error("membership could not be created")
    }
  }

  def createSubscription(
    org: Organization,
    user: User = Util.createRandomUser(),
    publication: Publication = Publication.all.head
  ): Subscription = {
    subscriptionsDao.create(
      Util.createdBy,
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = publication
      )
    )
  }

  def createService(app: io.apibuilder.api.v0.models.Application): spec.Service = spec.Service(
    apidoc = spec.Apidoc(version = io.apibuilder.spec.v0.Constants.Version),
    info = spec.Info(contact = None, license = None),
    name = app.name,
    organization = spec.Organization(key = app.organization.key),
    application = spec.Application(key = app.key),
    namespace = "test." + app.key,
    version = "0.0.1-dev",
    headers = Nil,
    imports = Nil,
    enums = Nil,
    models = Nil,
    unions = Nil,
    resources = Nil
  )


  lazy val createdBy = {
    play.api.Play.current.injector.instanceOf[db.UsersDao].AdminUser
  }

  lazy val gilt = upsertOrganization("Gilt Test Org")

  lazy val testOrg = upsertOrganization("Test Org %s".format(UUID.randomUUID))

}
