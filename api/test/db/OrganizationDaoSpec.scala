package db

import org.scalatest.{ FunSpec, Matchers }
import org.junit.Assert._
import java.util.UUID

class OrganizationDaoSpec extends FunSpec with Matchers {

  it("create") {
    assertEquals(Util.gilt.name, "Gilt")
    assertEquals(Util.gilt.key, "gilt")
  }

  it("user that creates org should be an admin") {
    val user = Util.upsertUser(UUID.randomUUID.toString + "@gilttest.com")
    val name = UUID.randomUUID.toString
    val org = OrganizationDao.createWithAdministrator(user, name)
    org.name should be(name)

    Membership.isUserAdmin(user, org) should be(true)
  }

  it("find by guid") {
    assertEquals(OrganizationDao.findByGuid(Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  it("findAll by key") {
    assertEquals(OrganizationDao.findAll(key = Some(Util.gilt.key)).head.key, Util.gilt.key)
  }

  describe("validation") {

    it("validates name") {
      OrganizationDao.validate("this is a long name") should be(Seq.empty)
      OrganizationDao.validate("a").head.message should be("name must be at least 4 characters")
    }

    it("raises error if you try to create an org with a short name") {
      intercept[java.lang.IllegalArgumentException] {
        OrganizationDao.createWithAdministrator(Util.createdBy, "a")
      }.getMessage should be("requirement failed: Name too short")
    }

    it("isDomainValid") {
      OrganizationDao.isDomainValid("gilt.com") should be(true)
      OrganizationDao.isDomainValid("gilt.org") should be(true)
      OrganizationDao.isDomainValid("www.gilt.com") should be(true)
      OrganizationDao.isDomainValid("WWW.GILT.COM") should be(true)
      OrganizationDao.isDomainValid("gilt-com") should be(false)
    }

  }

}
