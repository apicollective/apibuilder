package db

import lib.Role
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class MembershipRequestsDaoSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))
  lazy val org = Util.createOrganization()
  lazy val member = Util.upsertUser("gilt-member@gilt.com")
  lazy val admin = Util.upsertUser("gilt-admin@gilt.com")

  it should "create member" in {
    val thisOrg = Util.createOrganization()

    assertEquals(MembershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), false)

    val request = MembershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Member)
    assertEquals(request.organization.name, thisOrg.name)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Member.key)

    assertEquals(MembershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), false)

    MembershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(MembershipsDao.isUserMember(member, thisOrg), true)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), false)
  }

  it should "create admin" in {
    val thisOrg = Util.createOrganization()

    assertEquals(MembershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), false)

    val request = MembershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Admin)
    assertEquals(request.organization.name, thisOrg.name)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Admin.key)

    assertEquals(MembershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), false)

    MembershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(MembershipsDao.isUserMember(member, thisOrg), true)
    assertEquals(MembershipsDao.isUserAdmin(member, thisOrg), true)
  }

  it should "findByGuid" in {
    val request = MembershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    assertEquals(request, MembershipRequestsDao.findByGuid(Authorization.All, request.guid).get)
  }

  it should "findAll for organization guid" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = MembershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), MembershipRequestsDao.findAll(Authorization.All, organizationGuid = Some(newOrg.guid)))
  }

  it should "findAll for organization key" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = MembershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), MembershipRequestsDao.findAll(Authorization.All, organizationKey = Some(newOrg.key)))
  }

  it should "findAllForUser" in {
    val newUser = Util.upsertUser(UUID.randomUUID().toString + "@gilttest.com")
    val newOrg = Util.createOrganization()

    val request1 = MembershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Admin)
    assertEquals(Seq(request1), MembershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid)))

    val request2 = MembershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Member)
    assertEquals(Seq(request2, request1), MembershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid)))
  }

  it can "softDelete" in {
    val request = MembershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    MembershipRequestsDao.softDelete(Util.createdBy, request)
    assertEquals(None, MembershipRequestsDao.findByGuid(Authorization.All, request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = Util.createOrganization()
    val request = MembershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))

    MembershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(member, MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member).get.user)
    assertEquals(
      "Accepted membership request for %s to join as Member".format(member.email),
      OrganizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

  it should "not create a membership record when declining" in {
    val newOrg = Util.createOrganization()
    val request = MembershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))

    MembershipRequestsDao.decline(Util.createdBy, request)
    assertEquals(None, MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))
    assertEquals(
      "Declined membership request for %s to join as Member".format(member.email),
      OrganizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

}
