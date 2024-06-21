package db

import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.common.v0.models.MembershipRole
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class MembershipRequestsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private[this] lazy val org: Organization = createOrganization()
  private[this] lazy val member: User = upsertUser("gilt-member@bryzek.com")
  
  "create member" in {
    val thisOrg = createOrganization()
  
    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  
    val request = membershipRequestsDao.upsert(testUser, thisOrg, member, MembershipRole.Member)
    request.organization.name must equal(thisOrg.name)
    request.user must equal(member)
    request.role must equal(MembershipRole.Member)
  
    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  
    membershipRequestsDao.accept(testUser, request)
    membershipsDao.isUserMember(member, thisOrg) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  }
  
  "create admin" in {
    val thisOrg = createOrganization()
  
    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  
    val request = membershipRequestsDao.upsert(testUser, thisOrg, member, MembershipRole.Admin)
    request.organization.name must equal(thisOrg.name)
    request.user must equal(member)
    request.role must equal(MembershipRole.Admin)
  
    membershipsDao.isUserMember(member, thisOrg) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(false)
  
    membershipRequestsDao.accept(testUser, request)
    membershipsDao.isUserMember(member, thisOrg) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg) must equal(true)
  }
  
  "findByGuid" in {
    val request = membershipRequestsDao.upsert(testUser, org, member, MembershipRole.Admin)
    membershipRequestsDao.findByGuid(Authorization.All, request.guid).get must equal(request)
  }
  
  "findAll for organization guid" in {
    val otherOrg = createOrganization()
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Admin)
    membershipRequestsDao.findAll(Authorization.All, organizationGuid = Some(newOrg.guid)) must equal(
      Seq(request)
    )
  }
  
  "findAll for organization key" in {
    createOrganization()
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Admin)
    Seq(request) must equal(
      membershipRequestsDao.findAll(Authorization.All, organizationKey = Some(newOrg.key))
    )
  }
  
  "findAllForUser" in {
    val newUser = upsertUser(UUID.randomUUID().toString + "@test.apibuilder.io")
    val newOrg = createOrganization()
  
    val request1 = membershipRequestsDao.upsert(testUser, newOrg, newUser, MembershipRole.Admin)
    Seq(request1) must equal(
      membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid))
    )
  
    val request2 = membershipRequestsDao.upsert(testUser, newOrg, newUser, MembershipRole.Member)
    Seq(request2, request1) must equal(
      membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid))
    )
  }
  
  "softDelete" in {
    val request = membershipRequestsDao.upsert(testUser, org, member, MembershipRole.Admin)
    membershipRequestsDao.softDelete(testUser, request)
    membershipRequestsDao.findByGuid(Authorization.All, request.guid).isEmpty must be(true)
  }
  
  "create a membership record when approving" in {
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Member)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, MembershipRole.Member) must be(None)

    membershipRequestsDao.accept(testUser, request)
    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, MembershipRole.Member).get.user must equal(member)

    organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message) must equal(
      Seq("Accepted membership request for %s to join as member".format(member.email))
    )
  }
  
  "not create a membership record when declining" in {
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Member)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, MembershipRole.Member) must be(None)
  
    membershipRequestsDao.decline(testUser, request)
    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg, member, MembershipRole.Member) must be(None)
    organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg), limit = 1).map(_.message) must equal(
      Seq("Declined membership request for %s to join as member".format(member.email))
    )
  }

}
