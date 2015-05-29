package db

import com.gilt.apidoc.api.v0.models.{Application, ApplicationForm, Organization, OrganizationForm, Original, OriginalType}
import com.gilt.apidoc.api.v0.models.{Publication, Subscription, SubscriptionForm, User, UserForm, Version, Visibility}
import com.gilt.apidoc.spec.v0.{models => spec}
import play.api.libs.json.{Json, JsObject}
import lib.Role
import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  def createRandomUser(): User = {
    val email = "random-user-" + UUID.randomUUID.toString + "@gilttest.com"
    UsersDao.create(UserForm(email = email, password = "test1"))
  }

  def upsertUser(
    email: String = "random-user-" + UUID.randomUUID.toString + "@gilttest.com",
    name: String = "Admin",
    password: String = "test1"
  ): User = {
    UsersDao.findByEmail(email).getOrElse {
      UsersDao.create(UserForm(email = email, name = Some(name), password = password))
    }
  }

  def upsertOrganization(name: String): Organization = {
    OrganizationsDao.findAll(Authorization.All, name = Some(name)).headOption.getOrElse {
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
      name = name.getOrElse(UUID.randomUUID.toString),
      key = key,
      namespace = namespace.getOrElse("test." + UUID.randomUUID.toString),
      visibility = visibility
    )
    OrganizationsDao.createWithAdministrator(createdBy, form)
  }

  def createOrganizationForm(
    name: String = UUID.randomUUID.toString,
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
    ApplicationsDao.create(Util.createdBy, org, form)
  }

  def createApplicationForm(
    name: String = "Test " + UUID.randomUUID.toString,
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
    VersionsDao.create(
      Util.createdBy,
      application,
      version,
      original,
      service.getOrElse { Util.createService(application) }
    )
  }

  def createOriginal(): Original = {
    com.gilt.apidoc.api.v0.models.Original(
      `type` = OriginalType.ApiJson,
      data = Json.obj(
        "apidoc" -> Json.obj(
          "version" -> com.gilt.apidoc.spec.v0.Constants.Version
        ),
        "name" -> s"test-${UUID.randomUUID}"
      ).toString
    )
  }

  def createMembership(
    org: Organization,
    user: User = Util.createRandomUser(),
    role: Role = Role.Admin
  ): com.gilt.apidoc.api.v0.models.Membership = {
    val request = MembershipRequestsDao.upsert(Util.createdBy, org, user, role)
    MembershipRequestsDao.accept(Util.createdBy, request)

    MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, org, user, role).getOrElse {
      sys.error("membership could not be created")
    }
  }

  def createSubscription(
    org: Organization,
    user: User = Util.createRandomUser(),
    publication: Publication = Publication.all.head
  ): Subscription = {
    SubscriptionsDao.create(
      Util.createdBy,
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = publication
      )
    )
  }

  def createService(app: com.gilt.apidoc.api.v0.models.Application): spec.Service = spec.Service(
    apidoc = spec.Apidoc(version = com.gilt.apidoc.spec.v0.Constants.Version),
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


  lazy val createdBy = UsersDao.AdminUser

  lazy val gilt = upsertOrganization("Gilt Test Org")

  lazy val testOrg = upsertOrganization("Test Org %s".format(UUID.randomUUID))

}
