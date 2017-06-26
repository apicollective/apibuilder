package db

import lib.Role
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class MembershipRequestsDaoSpec extends FlatSpec with util.TestApplication {

  lazy val org = Util.createOrganization()
  lazy val member = Util.upsertUser("gilt-member@bryzek.com")
  lazy val admin = Util.upsertUser("gilt-admin@bryzek.com")

  it should "create member" in {
    val thisOrg = Util.createOrganization()

    assertEquals(membershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), false)

    val request = membershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Member)
    assertEquals(request.organization.name, thisOrg.name)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Member.key)

    assertEquals(membershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), false)

    membershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(membershipsDao.isUserMember(member, thisOrg), true)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), false)
  }

  it should "create admin" in {
    val thisOrg = Util.createOrganization()

    assertEquals(membershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), false)

    val request = membershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Admin)
    assertEquals(request.organization.name, thisOrg.name)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Admin.key)

    assertEquals(membershipsDao.isUserMember(member, thisOrg), false)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), false)

    membershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(membershipsDao.isUserMember(member, thisOrg), true)
    assertEquals(membershipsDao.isUserAdmin(member, thisOrg), true)
  }

  it should "findByGuid" in {
    val request = membershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    assertEquals(request, membershipRequestsDao.findByGuid(Authorization.All, request.guid).get)
  }

  it should "findAll for organization guid" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), membershipRequestsDao.findAll(Authorization.All, organizationGuid = Some(newOrg.guid)))
  }

  it should "findAll for organization key" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    assertEquals(Seq(request), membershipRequestsDao.findAll(Authorization.All, organizationKey = Some(newOrg.key)))
  }

  it should "findAllForUser" in {
    val newUser = Util.upsertUser(UUID.randomUUID().toString + "@test.apibuilder.io")
    val newOrg = Util.createOrganization()

    val request1 = membershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Admin)
    assertEquals(Seq(request1), membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid)))

    val request2 = membershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Member)
    assertEquals(Seq(request2, request1), membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid)))
  }

  it can "softDelete" in {
    val request = membershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    membershipRequestsDao.softDelete(Util.createdBy, request)
    assertEquals(None, membershipRequestsDao.findByGuid(Authorization.All, request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))

    membershipRequestsDao.accept(Util.createdBy, request)
    assertEquals(member, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member).get.user)
    assertEquals(
      "Accepted membership request for %s to join as Member".format(member.email),
      organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

  it should "not create a membership record when declining" in {
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    assertEquals(None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))

    membershipRequestsDao.decline(Util.createdBy, request)
    assertEquals(None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, Role.Member))
    assertEquals(
      "Declined membership request for %s to join as Member".format(member.email),
      organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

}
