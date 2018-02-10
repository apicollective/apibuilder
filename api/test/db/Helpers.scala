package db

import io.apibuilder.api.v0.models._
import io.apibuilder.spec.v0.{models => spec}
import play.api.libs.json.Json
import lib.Role
import java.util.UUID

trait Helpers extends util.Daos {

  def createRandomName(suffix: String): String = {
    s"z-test-$suffix-" + UUID.randomUUID.toString
  }

  def createRandomUser(): User = {
    val email = "random-user-" + UUID.randomUUID.toString + "@test.apibuilder.io"
    usersDao.create(UserForm(email = email, password = "test1"))
  }

  def upsertUser(
    email: String = "random-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
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
    createdBy: User = testUser,
    name: Option[String] = None,
    key: Option[String] = None,
    namespace: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ): Organization = {
    createOrganization(
      form = createOrganizationForm(
        name = name.getOrElse("z-test-org-" + UUID.randomUUID.toString),
        key = key,
        namespace = namespace.getOrElse("test." + UUID.randomUUID.toString),
        visibility = visibility
      ),
      createdBy = createdBy
    )
  }

  def createOrganization(
    form: OrganizationForm,
    createdBy: User
  ): Organization = {
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
    applicationsDao.create(testUser, org, form)
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

  def createAttributeForm(
    name: String = createRandomName("attribute"),
    description: Option[String] = None
  ) = AttributeForm(
    name = name,
    description = description
  )

  def createVersion(
    application: Application = createApplication(),
    version: String = "1.0.0",
    original: Original = createOriginal(),
    service: Option[spec.Service] = None
  ): Version = {
    versionsDao.create(
      testUser,
      application,
      version,
      original,
      service.getOrElse { createService(application) }
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
    user: User = createRandomUser(),
    role: Role = Role.Admin
  ): io.apibuilder.api.v0.models.Membership = {
    val request = membershipRequestsDao.upsert(testUser, org, user, role)
    membershipRequestsDao.accept(testUser, request)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, org, user, role).getOrElse {
      sys.error("membership could not be created")
    }
  }

  def createSubscription(
    org: Organization,
    user: User = createRandomUser(),
    publication: Publication = Publication.all.head
  ): Subscription = {
    createSubscription(
      user,
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = publication
      )
    )
  }

  def createSubscription(
    user: User,
    form: SubscriptionForm
  ): Subscription = {
    subscriptionsDao.create(testUser, form)
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

  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    usersDao.create(form)
  }

  def createUserForm() = UserForm(
    email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
    password = UUID.randomUUID.toString,
    name = None
  )

  lazy val gilt: Organization = upsertOrganization("Gilt Test Org")

  lazy val testOrg: Organization = upsertOrganization("Test Org %s".format(UUID.randomUUID))

  lazy val testUser: User = createUser()

}
