package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class MembershipRequestSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val createdBy = User.upsert("otto@gilt.com")
  lazy val member = User.upsert("gilt-member@gilt.com")
  lazy val admin = User.upsert("gilt-admin@gilt.com")
  lazy val gilt = {
    Organization.findByKey("gilt").getOrElse {
      Organization.create(createdBy, "Gilt")
    }
  }

  it should "create member" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, "member")
    assertEquals(request.org, gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, "member")
  }

  it should "create admin" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, "admin")
    assertEquals(request.org, gilt)
    assertEquals(request.user, member)
    assertEquals(request.role, "admin")
  }

  it should "findByGuid" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, "admin")
    assertEquals(request, MembershipRequest.findByGuid(request.guid).get)
  }

  it should "findAllForOrganization" in {
    val newOrg = Organization.create(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, "admin")
    assertEquals(Seq(request), MembershipRequest.findAllForOrganization(newOrg))
  }

  it should "findAllForUser" in {
    val newUser = User.upsert(UUID.randomUUID().toString + "@gilttest.com")
    val newOrg = Organization.create(createdBy, UUID.randomUUID().toString)

    val request1 = MembershipRequest.upsert(createdBy, newOrg, newUser, "admin")
    assertEquals(Seq(request1), MembershipRequest.findAllForUser(newUser))

    val request2 = MembershipRequest.upsert(createdBy, newOrg, newUser, "member")
    assertEquals(Seq(request2, request1), MembershipRequest.findAllForUser(newUser))
  }

  it can "softDelete" in {
    val request = MembershipRequest.upsert(createdBy, gilt, member, "admin")
    MembershipRequest.softDelete(createdBy, request.guid)
    assertEquals(None, MembershipRequest.findByGuid(request.guid))
  }

  it should "create a membership record when approving" in {
    val newOrg = Organization.create(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, "member")

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, "member"))

    request.approve(createdBy)
    assertEquals(member, Membership.findByOrganizationAndUserAndRole(newOrg, member, "member").get.user)
    assertEquals("Approved membership request for %s to join as member".format(member.email),
                 MembershipLog.findAllForOrganization(newOrg).map(_.message).head)
  }

  it should "not create a membership record when declining" in {
    val newOrg = Organization.create(createdBy, UUID.randomUUID().toString)
    val request = MembershipRequest.upsert(createdBy, newOrg, member, "member")

    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, "member"))

    request.decline(createdBy)
    assertEquals(None, Membership.findByOrganizationAndUserAndRole(newOrg, member, "member"))
    assertEquals("Declined membership request for %s to join as member".format(member.email),
                 MembershipLog.findAllForOrganization(newOrg).map(_.message).head)
  }

}
