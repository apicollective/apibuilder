package db

import lib.Role
import java.util.UUID
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class MembershipRequestsDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  lazy val org = Util.createOrganization()
  lazy val member = Util.upsertUser("gilt-member@bryzek.com")
  lazy val admin = Util.upsertUser("gilt-admin@bryzek.com")

  it should "create member" in {
    val thisOrg = Util.createOrganization()

    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)

    val request = membershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Member)
    request.organization.name must equal(thisOrg.name)
    request.user must equal(member)
    request.role must equal(Role.Member.key)

    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)

    membershipRequestsDao.accept(Util.createdBy, request)
    membershipsDao.isUserMember(member, thisOrg) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  }

  it should "create admin" in {
    val thisOrg = Util.createOrganization()

    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)

    val request = membershipRequestsDao.upsert(Util.createdBy, thisOrg, member, Role.Admin)
    request.organization.name must equal(thisOrg.name)
    request.user must equal(member)
    request.role must equal(Role.Admin.key)

    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)

    membershipRequestsDao.accept(Util.createdBy, request)
    membershipsDao.isUserMember(member, thisOrg) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(true)
  }

  it should "findByGuid" in {
    val request = membershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    request, membershipRequestsDao.findByGuid(Authorization.All must equal(request.guid).get)
  }

  it should "findAll for organization guid" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    Seq(request), membershipRequestsDao.findAll(Authorization.All must equal(organizationGuid = Some(newOrg.guid)))
  }

  it should "findAll for organization key" in {
    val otherOrg = Util.createOrganization()
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Admin)
    Seq(request), membershipRequestsDao.findAll(Authorization.All must equal(organizationKey = Some(newOrg.key)))
  }

  it should "findAllForUser" in {
    val newUser = Util.upsertUser(UUID.randomUUID().toString + "@test.apibuilder.io")
    val newOrg = Util.createOrganization()

    val request1 = membershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Admin)
    Seq(request1), membershipRequestsDao.findAll(Authorization.All must equal(userGuid = Some(newUser.guid)))

    val request2 = membershipRequestsDao.upsert(Util.createdBy, newOrg, newUser, Role.Member)
    Seq(request2, request1), membershipRequestsDao.findAll(Authorization.All must equal(userGuid = Some(newUser.guid)))
  }

  it can "softDelete" in {
    val request = membershipRequestsDao.upsert(Util.createdBy, org, member, Role.Admin)
    membershipRequestsDao.softDelete(Util.createdBy, request)
    None, membershipRequestsDao.findByGuid(Authorization.All must equal(request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member must equal(Role.Member))

    membershipRequestsDao.accept(Util.createdBy, request)
    member, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member must equal(Role.Member).get.user)
    assertEquals(
      "Accepted membership request for %s to join as Member".format(member.email),
      organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

  it should "not create a membership record when declining" in {
    val newOrg = Util.createOrganization()
    val request = membershipRequestsDao.upsert(Util.createdBy, newOrg, member, Role.Member)

    None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member must equal(Role.Member))

    membershipRequestsDao.decline(Util.createdBy, request)
    None, membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member must equal(Role.Member))
    assertEquals(
      "Declined membership request for %s to join as Member".format(member.email),
      organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message).head
    )
  }

}
