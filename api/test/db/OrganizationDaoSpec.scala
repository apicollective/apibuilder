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

    org.domains.map(_.name).mkString(" ") should be(domains.mkString(" "))

    val fetched = OrganizationDao.findByGuid(org.guid).get
    fetched.domains.map(_.name).sorted.mkString(" ") should be(domains.sorted.mkString(" "))
  }

  it("creates with metadata") {
    val org = OrganizationDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        metadata = Some(OrganizationMetadataForm(
          package_name = Some("com.gilt")
        ))
      )
    )

    org.metadata.package_name should be(Some("com.gilt"))
    val fetched = OrganizationDao.findByGuid(org.guid).get
    fetched.metadata.package_name should be(Some("com.gilt"))
  }

  it("find by guid") {
    assertEquals(OrganizationDao.findByGuid(Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  it("findAll by key") {
    assertEquals(OrganizationDao.findAll(key = Some(Util.gilt.key)).head.key, Util.gilt.key)
  }

  it("emailDomain") {
    OrganizationDao.emailDomain("mb@gilt.com") should be(Some("gilt.com"))
    OrganizationDao.emailDomain("mb@internal.gilt.com") should be(Some("internal.gilt.com"))
    OrganizationDao.emailDomain("mb") should be(None)
  }

  describe("validation") {

    it("validates name") {
      OrganizationDao.validate(OrganizationForm(name = "this is a long name")) should be(Seq.empty)
      OrganizationDao.validate(OrganizationForm(name = "a")).head.message should be("name must be at least 4 characters")
      OrganizationDao.validate(OrganizationForm(name = Util.gilt.name)).head.message should be("Org with this name already exists")
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

    it("validates domains") {
      val name = UUID.randomUUID.toString
      OrganizationDao.validate(OrganizationForm(name = name, domains = None)) should be(Seq.empty)
      OrganizationDao.validate(OrganizationForm(name = name, domains = Some(Seq.empty))) should be(Seq.empty)
      OrganizationDao.validate(OrganizationForm(name = name, domains = Some(Seq("bad name")))).head.message should be("Domain bad name is not valid. Expected a domain name like gilt.com")
    }
  }

}
