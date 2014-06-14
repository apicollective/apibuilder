package db

import core.Role
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class MembershipRequestSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val member = UserDao.upsert("gilt-member@gilt.com")
  lazy val admin = UserDao.upsert("gilt-admin@gilt.com")

  it should "create member" in {
    val request = MembershipRequest.upsert(Util.createdBy, Util.gilt, member, Role.Member)
    assertEquals(request.organization, Util.gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Member.key)
  }

  it should "create admin" in {
    val request = MembershipRequest.upsert(Util.createdBy, Util.gilt, member, Role.Admin)
    assertEquals(request.organization, Util.gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Admin.key)
  }

  it should "findByGuid" in {
    val request = MembershipRequest.upsert(Util.createdBy, Util.gilt, member, Role.Admin)
    assertEquals(request, MembershipRequest.findByGuid(request.guid).get)
  }

  it should "findAll for organization guid" in {
    val otherOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val newOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), MembershipRequest.findAll(organizationGuid = Some(newOrg.guid)))
  }

  it should "findAll for organization key" in {
    val otherOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val newOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), MembershipRequest.findAll(organizationKey = Some(newOrg.key)))
  }

  it should "findAllForUser" in {
    val newUser = UserDao.upsert(UUID.randomUUID().toString + "@gilttest.com")
    val newOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)

    val request1 = MembershipRequest.upsert(Util.createdBy, newOrg, newUser, Role.Admin)
    assertEquals(Seq(request1), MembershipRequest.findAll(userGuid = Some(newUser.guid)))

    val request2 = MembershipRequest.upsert(Util.createdBy, newOrg, newUser, Role.Member)
    assertEquals(Seq(request2, request1), MembershipRequest.findAll(userGuid = Some(newUser.guid)))
  }

  it can "softDelete" in {
    val request = MembershipRequest.upsert(Util.createdBy, Util.gilt, member, Role.Admin)
    MembershipRequest.softDelete(Util.createdBy, request)
    assertEquals(None, MembershipRequest.findByGuid(request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member))

    request.accept(Util.createdBy)
    assertEquals(member, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member).get.user)
    assertEquals("Accepted membership request for %s to join as Member".format(member.email),
                 OrganizationLog.findAllForOrganization(newOrg).map(_.message).head)
  }

  it should "not create a membership record when declining" in {
    val newOrg = OrganizationDao.createWithAdministrator(Util.createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member))

    request.decline(Util.createdBy)
    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member))
    assertEquals("Declined membership request for %s to join as Member".format(member.email),
                 OrganizationLog.findAllForOrganization(newOrg).map(_.message).head)
  }

}
