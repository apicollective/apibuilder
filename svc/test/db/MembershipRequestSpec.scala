package db

import core.{ Role, User }
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class MembershipRequestSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val createdBy = UserDao.upsert("otto@gilt.com")
  lazy val member = UserDao.upsert("gilt-member@gilt.com")
  lazy val admin = UserDao.upsert("gilt-admin@gilt.com")
  lazy val gilt = {
    OrganizationDao.findByUserAndKey(createdBy, "gilt").getOrElse {
      OrganizationDao.createWithAdministrator(createdBy, "Gilt")
    }
  }

  it should "create member" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, Role.Member.key)
    assertEquals(request.org, gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Member.key)
  }

  it should "create admin" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, Role.Admin.key)
    assertEquals(request.org, gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, Role.Admin.key)
  }

  it should "findByGuid" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, Role.Admin.key)
    assertEquals(request, MembershipRequest.findByGuid(request.guid).get)
  }

  it should "findAllForOrganization" in {
    val newOrg = OrganizationDao.createWithAdministrator(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, Role.Admin.key)
    assertEquals(Seq(request), MembershipRequest.findAllForOrganization(newOrg))
  }

  it should "findAllForUser" in {
    val newUser = UserDao.upsert(UUID.randomUUID().toString + "@gilttest.com")
    val newOrg = OrganizationDao.createWithAdministrator(createdBy, UUID.randomUUID().toString)

    val request1 = MembershipRequest.upsert(createdBy, newOrg, newUser, Role.Admin.key)
    assertEquals(Seq(request1), MembershipRequest.findAllForUser(newUser))

    val request2 = MembershipRequest.upsert(createdBy, newOrg, newUser, Role.Member.key)
    assertEquals(Seq(request2, request1), MembershipRequest.findAllForUser(newUser))
  }

  it can "softDelete" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, Role.Admin.key)
    MembershipRequest.softDelete(createdBy, request.guid)
    assertEquals(None, MembershipRequest.findByGuid(request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = OrganizationDao.createWithAdministrator(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, Role.Member.key)

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member.key))

    request.approve(createdBy)
    assertEquals(member, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member.key).get.user)
    assertEquals("Approved membership request for %s to join as member".format(member.email),
                 OrganizationLog.findAllForOrganization(newOrg).map(_.message).head)
  }

  it should "not create a membership record when declining" in {
    val newOrg = OrganizationDao.createWithAdministrator(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, Role.Member.key)

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member.key))

    request.decline(createdBy)
    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, Role.Member.key))
    assertEquals("Declined membership request for %s to join as member".format(member.email),
                 OrganizationLog.findAllForOrganization(newOrg).map(_.message).head)
  }

}
