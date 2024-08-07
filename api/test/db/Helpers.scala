package db

import helpers.{RandomHelpers, ValidatedTestHelpers}
import io.apibuilder.api.v0.models._
import io.apibuilder.common.v0.models.MembershipRole
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.{models => spec}
import models.{MembershipRequestsModel, VersionsModel}
import play.api.libs.json.Json

import java.util.UUID

trait Helpers extends util.Daos with RandomHelpers with ValidatedTestHelpers {
  def versionsModel: VersionsModel = injector.instanceOf[VersionsModel]
  private def membershipRequestsModel: MembershipRequestsModel = injector.instanceOf[MembershipRequestsModel]

  def createRandomUser(): InternalUser = {
    val email = "random-user-" + UUID.randomUUID.toString + "@test.apibuilder.io"
    expectValid {
      usersDao.create(UserForm(email = email, password = "test1"))
    }
  }

  def upsertUser(
    email: String = "random-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
    name: String = "Admin",
    password: String = "test1"
  ): InternalUser = {
    usersDao.findByEmail(email).getOrElse {
      expectValid {
        usersDao.create(UserForm(email = email, name = Some(name), password = password))
      }
    }
  }

  def upsertOrganization(name: String): InternalOrganization = {
    organizationsDao.findAll(Authorization.All, name = Some(name), limit = Some(1)).headOption.getOrElse {
      createOrganization(name = Some(name))
    }
  }

  def upsertOrganizationByKey(key: String): InternalOrganization = {
    organizationsDao.findByKey(Authorization.All, key).getOrElse {
      createOrganization(key = Some(key))
    }
  }

  def createOrganization(
    createdBy: InternalUser = testUser,
    name: Option[String] = None,
    key: Option[String] = None,
    namespace: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ): InternalOrganization = {
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
    createdBy: InternalUser
  ): InternalOrganization = {
    organizationsDao.createWithAdministrator(createdBy, form)
  }

  def createOrganizationForm(
    name: String = "z-test-org-" + UUID.randomUUID.toString,
    key: Option[String] = None,
    namespace: String = "test." + UUID.randomUUID.toString,
    visibility: Visibility = Visibility.Organization,
    domains: Option[Seq[String]] = None
  ): OrganizationForm = OrganizationForm(
    name = name,
    key = key,
    namespace = namespace,
    visibility = visibility,
    domains = domains
  )

  def createApplication(
    org: InternalOrganization = createOrganization(),
    form: ApplicationForm = createApplicationForm()
  ): InternalApplication = {
    applicationsDao.create(testUser, org, form)
  }

  def createApplicationForm(
    name: String = "z-test-app-" + UUID.randomUUID.toString,
    key: Option[String] = None,
    description: Option[String] = None,
    visibility: Visibility = Visibility.Organization
  ): ApplicationForm = ApplicationForm(
    name = name,
    key = key,
    description = description,
    visibility = visibility
  )

  def createAttributeForm(
    name: String = createRandomName("attribute"),
    description: Option[String] = None
  ): AttributeForm = AttributeForm(
    name = name,
    description = description
  )

  def upsertApplicationByOrganizationAndKey(
    org: InternalOrganization,
    key: String,
  ): InternalApplication = {
    applicationsDao.findByOrganizationKeyAndApplicationKey(
      Authorization.All, org.key, key,
    ).getOrElse {
      createApplication(
        org = org,
        form = createApplicationForm().copy(key = Some(key))
      )
    }
  }

  def createVersion(service: Service): Version = {
    val org = upsertOrganizationByKey(service.organization.key)
    val application = upsertApplicationByOrganizationAndKey(org, service.application.key)
    val v = versionsDao.create(testUser, application, service.version, createOriginal(service), service)
    versionsModel.toModel(v).get
  }

  def createApplicationByKey(
    org: InternalOrganization = testOrg,
    key: String = "test-" + UUID.randomUUID.toString,
  ): InternalApplication = {
    createApplication(
      org = org,
      form = createApplicationForm().copy(key = Some(key))
    )
  }

  def createVersion(
    application: InternalApplication = createApplication(),
    version: String = "1.0.0",
    original: Original = createOriginal(),
    service: Option[spec.Service] = None
  ): Version = {
    versionsModel.toModel(
      versionsDao.create(
        testUser,
        application,
        version,
        original,
        service.getOrElse { createService(application) }
      )
    ).get
  }

  def createMembership(
    org: InternalOrganization,
    user: InternalUser = createRandomUser(),
    role: MembershipRole = MembershipRole.Admin
  ): InternalMembership = {
    val request = membershipRequestsModel.toModel(
      membershipRequestsDao.upsert(testUser, org, user, role)
    ).get
    membershipRequestsDao.accept(testUser, request)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, org.reference, user.reference, role).getOrElse {
      sys.error("membership could not be created")
    }
  }

  def createSubscription(
    org: InternalOrganization,
    user: InternalUser = createRandomUser(),
    publication: Publication = Publication.all.head
  ): InternalSubscription = {
    createSubscription(
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = publication
      )
    )
  }

  def createSubscription(
    form: SubscriptionForm
  ): InternalSubscription = {
    expectValid {
      subscriptionsDao.create(testUser, form)
    }
  }

  def createService(app: InternalApplication): spec.Service = {
    val org = organizationsDao.findByGuid(Authorization.All, app.organizationGuid).get
    spec.Service(
      info = spec.Info(contact = None, license = None),
      name = app.name,
      organization = spec.Organization(key = org.key),
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
  }

  def createOriginal(svc: spec.Service): io.apibuilder.api.v0.models.Original = {
    createOriginal(name = svc.name)
  }

  def createOriginal(name: String = s"test-${UUID.randomUUID}"): io.apibuilder.api.v0.models.Original = {
    io.apibuilder.api.v0.models.Original(
      `type` = OriginalType.ApiJson,
      data = Json.obj(
        "name" -> name
      ).toString
    )
  }

  def createUser(
    form: UserForm = makeUserForm()
  ): InternalUser = {
    expectValid {
      usersDao.create(form)
    }
  }

  def makeUserForm(): UserForm = UserForm(
    email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
    password = UUID.randomUUID.toString,
    name = None
  )

  lazy val gilt: InternalOrganization = upsertOrganization("Gilt Test Org")

  lazy val testOrg: InternalOrganization = upsertOrganization("Test Org %s".format(UUID.randomUUID))

  lazy val testUser: InternalUser = createUser()

}
