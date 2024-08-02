package db

import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.common.v0.models.MembershipRole
import models.MembershipRequestsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class InternalMembershipRequestsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private lazy val org: InternalOrganization = createOrganization()
  private lazy val member: InternalUser = upsertUser("gilt-member@bryzek.com")
  private def membershipRequestsModel: MembershipRequestsModel = app.injector.instanceOf[MembershipRequestsModel]
  
  "create member" in {
    val thisOrg = createOrganization()
  
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(false)
  
    val request = membershipRequestsDao.upsert(testUser, thisOrg, member, MembershipRole.Member)
    request.organizationGuid must equal(thisOrg.guid)
    request.userGuid must equal(member.guid)
    request.role must equal(MembershipRole.Member)
  
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(false)
  
    membershipRequestsDao.accept(testUser, membershipRequestsModel.toModel(request).value)
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(false)
  }
  
  "create admin" in {
    val thisOrg = createOrganization()
  
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(false)
  
    val request = membershipRequestsDao.upsert(testUser, thisOrg, member, MembershipRole.Admin)
    request.organizationGuid must equal(thisOrg.guid)
    request.userGuid must equal(member.guid)
    request.role must equal(MembershipRole.Admin)
  
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(false)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(false)
  
    membershipRequestsDao.accept(testUser, membershipRequestsModel.toModel(request).value)
    membershipsDao.isUserMember(member, thisOrg.reference) must equal(true)
    membershipsDao.isUserAdmin(member, thisOrg.reference) must equal(true)
  }
  
  "findByGuid" in {
    val request = membershipRequestsDao.upsert(testUser, org, member, MembershipRole.Admin)
    membershipRequestsDao.findByGuid(Authorization.All, request.guid).get must equal(request)
  }
  
  "findAll for organization guid" in {
    createOrganization() // create another org for testing
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Admin)
    membershipRequestsDao.findAll(Authorization.All, organizationGuid = Some(newOrg.guid), limit = None) must equal(
      Seq(request)
    )
  }
  
  "findAll for organization key" in {
    createOrganization()
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Admin)
    Seq(request) must equal(
      membershipRequestsDao.findAll(Authorization.All, organizationKey = Some(newOrg.key), limit = None)
    )
  }
  
  "findAllForUser" in {
    val newUser = upsertUser(UUID.randomUUID().toString + "@test.apibuilder.io")
    val newOrg = createOrganization()
  
    val request1 = membershipRequestsDao.upsert(testUser, newOrg, newUser, MembershipRole.Admin)
    Seq(request1) must equal(
      membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid), limit = None)
    )
  
    val request2 = membershipRequestsDao.upsert(testUser, newOrg, newUser, MembershipRole.Member)
    Seq(request2, request1) must equal(
      membershipRequestsDao.findAll(Authorization.All, userGuid = Some(newUser.guid), limit = None)
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

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg.reference, member.reference, MembershipRole.Member) must be(None)

    membershipRequestsDao.accept(testUser, membershipRequestsModel.toModel(request).value)
    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg.reference, member.reference, MembershipRole.Member).get.userGuid must equal(member.guid)

    organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg.reference), limit = 1).map(_.message) must equal(
      Seq("Accepted membership request for %s to join as member".format(member.email))
    )
  }
  
  "not create a membership record when declining" in {
    val newOrg = createOrganization()
    val request = membershipRequestsDao.upsert(testUser, newOrg, member, MembershipRole.Member)

    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg.reference, member.reference, MembershipRole.Member) must be(None)
  
    membershipRequestsDao.decline(testUser, membershipRequestsModel.toModel(request).value)
    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, newOrg.reference, member.reference, MembershipRole.Member) must be(None)
    organizationLogsDao.findAll(Authorization.All, organization = Some(newOrg.reference), limit = 1).map(_.message) must equal(
      Seq("Declined membership request for %s to join as member".format(member.email))
    )
  }

}
