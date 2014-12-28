package db

import com.gilt.apidoc.models.{Organization, OrganizationForm, Publication, Subscription, SubscriptionForm, User, Visibility}
import lib.Role
import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  def createRandomUser(): User = {
    val email = "random-user-" + UUID.randomUUID.toString + "@gilttest.com"
    UsersDao.create(UserForm(email = email, name = None, password = "test1"))
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
    key: Option[String] = None
  ): Organization = {
    val form = OrganizationForm(
      name = name.getOrElse(UUID.randomUUID.toString),
      key = key
    )
    OrganizationsDao.createWithAdministrator(createdBy, form)
  }

  def createMembership(
    org: Organization,
    user: User = Util.createRandomUser(),
    role: Role = Role.Admin
  ): com.gilt.apidoc.models.Membership = {
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

  lazy val createdBy = Util.upsertUser("admin@apidoc.me")

  lazy val gilt = upsertOrganization("Gilt Test Org")

  lazy val testOrg = upsertOrganization("Test Org %s".format(UUID.randomUUID))

}
