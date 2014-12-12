package db

import com.gilt.apidoc.models.{Organization, OrganizationForm, Publication, Service, Subscription, SubscriptionForm, User, Visibility}
import lib.Role
import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  def createRandomUser(): User = {
    val email = "random-user-" + UUID.randomUUID.toString + "@gilttest.com"
    UserDao.create(UserForm(email = email, name = None, password = "test"))
  }

  def upsertUser(email: String): User = {
    UserDao.findByEmail(email).getOrElse {
      UserDao.create(UserForm(email = email, name = Some("Admin"), password = "test"))
    }
  }

  def upsertOrganization(name: String): Organization = {
    OrganizationDao.findAll(Authorization.All, name = Some(name)).headOption.getOrElse {
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
    OrganizationDao.createWithAdministrator(createdBy, form)
  }

  def createMembership(
    org: Organization,
    user: User = Util.createRandomUser(),
    role: Role = Role.Admin
  ): com.gilt.apidoc.models.Membership = {
    val request = MembershipRequestDao.upsert(Util.createdBy, org, user, role)
    MembershipRequestDao.accept(Util.createdBy, request)

    Membership.findByOrganizationAndUserAndRole(org, user, role).getOrElse {
      sys.error("membership could not be created")
    }
  }

  def createSubscription(
    org: Organization,
    user: User = Util.createRandomUser(),
    publication: Publication = Publication.all.head
  ): Subscription = {
    SubscriptionDao.create(
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
