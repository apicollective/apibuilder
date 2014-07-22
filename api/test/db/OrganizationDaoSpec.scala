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
    val org = OrganizationDao.createWithAdministrator(user, OrganizationForm(name = name))
    org.name should be(name)

    Membership.isUserAdmin(user, org) should be(true)
  }

  it("creates with domains") {
    val domains = Seq(UUID.randomUUID.toString + ".com", UUID.randomUUID.toString + ".org")
    val org = OrganizationDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        domains = Some(domains)
      )
    )

    org.domains.mkString(" ") should be(domains.mkString(" "))

    val fetched = OrganizationDao.findByGuid(org.guid).get
    fetched.domains.mkString(" ") should be(domains.mkString(" "))
  }

  it("find by guid") {
    assertEquals(OrganizationDao.findByGuid(Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  it("findAll by key") {
    assertEquals(OrganizationDao.findAll(key = Some(Util.gilt.key)).head.key, Util.gilt.key)
  }

  describe("validation") {

    it("validates name") {
      OrganizationDao.validate(OrganizationForm(name = "this is a long name")) should be(Seq.empty)
      OrganizationDao.validate(OrganizationForm(name = "a")).head.message should be("name must be at least 4 characters")
    }

    it("raises error if you try to create an org with a short name") {
      intercept[java.lang.IllegalArgumentException] {
        OrganizationDao.createWithAdministrator(Util.createdBy, OrganizationForm("a"))
      }.getMessage should be("requirement failed: Name too short")
    }

    it("isDomainValid") {
      OrganizationDao.isDomainValid("gilt.com") should be(true)
      OrganizationDao.isDomainValid("gilt.org") should be(true)
      OrganizationDao.isDomainValid("www.gilt.com") should be(true)
      OrganizationDao.isDomainValid("WWW.GILT.COM") should be(true)
      OrganizationDao.isDomainValid("www gilt com") should be(false)
    }

  }

}
