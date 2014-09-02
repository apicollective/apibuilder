package db

import com.gilt.apidoc.models.Visibility
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class OrganizationDaoSpec extends FunSpec with Matchers {

  it("create") {
    Util.gilt.name should be("Gilt Test Org")
    Util.gilt.key should be("gilt-test-org")
  }

  it("user that creates org should be an admin") {
    val user = Util.upsertUser(UUID.randomUUID.toString + "@gilttest.com")
    val name = UUID.randomUUID.toString
    val org = OrganizationDao.createWithAdministrator(user, OrganizationForm(name = name))
    org.name should be(name)

    Membership.isUserAdmin(user, org) should be(true)
  }

  it("reverseDomain") {
    OrganizationDao.reverseDomain("gilt.com") should be("com.gilt")
    OrganizationDao.reverseDomain("foo.gilt.com") should be("com.gilt.foo")

  }

  describe("domain") {

    val domainName = UUID.randomUUID.toString
    val domains = Seq(domainName + ".com", UUID.randomUUID.toString + ".org")
    val org = OrganizationDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        domains = Some(domains)
      )
    )

    it("creates with domains") {
      org.domains.map(_.name).mkString(" ") should be(domains.mkString(" "))
      val fetched = OrganizationDao.findByGuid(Authorization.All, org.guid).get
      fetched.domains.map(_.name).sorted.mkString(" ") should be(domains.sorted.mkString(" "))
    }

    it("defaults metadata.package_name to reverse of first domain if provided") {
      val fetched = OrganizationDao.findByGuid(Authorization.All, org.guid).get
      fetched.metadata.get.packageName should be(Some("com." + domainName))
    }

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

    org.metadata.get.packageName should be(Some("com.gilt"))
    val fetched = OrganizationDao.findByGuid(Authorization.All, org.guid).get
    fetched.metadata.get.packageName should be(Some("com.gilt"))
  }

  it("find by guid") {
    assertEquals(OrganizationDao.findByGuid(Authorization.All, Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  describe("findAll") {

    val user1 = Util.createRandomUser()
    val org1 = Util.createOrganization(user1)

    val user2 = Util.createRandomUser()
    val org2 = Util.createOrganization(user2)

    it("by key") {
      OrganizationDao.findAll(Authorization.All, key = Some(org1.key)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, key = Some(org2.key)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by name") {
      OrganizationDao.findAll(Authorization.All, name = Some(org1.name)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, name = Some(org1.name.toUpperCase)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, name = Some(org1.name.toLowerCase)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, name = Some(org2.name)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by guid") {
      OrganizationDao.findAll(Authorization.All, guid = Some(org1.guid)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, guid = Some(org2.guid)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by userGuid") {
      OrganizationDao.findAll(Authorization.All, userGuid = Some(user1.guid)).map(_.guid) should be(Seq(org1.guid))
      OrganizationDao.findAll(Authorization.All, userGuid = Some(user2.guid)).map(_.guid) should be(Seq(org2.guid))
    }
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

  describe("Authorization") {

    val publicUser = Util.createRandomUser()
    val publicOrg = Util.createOrganization(publicUser, Some("Public " + UUID.randomUUID().toString))
    OrganizationMetadataDao.create(Util.createdBy, publicOrg, OrganizationMetadataForm(visibility = Some(Visibility.Public.toString)))

    val privateUser = Util.createRandomUser()
    val privateOrg = Util.createOrganization(privateUser, Some("Private " + UUID.randomUUID().toString))

    describe("All") {

      it("sees both orgs") {
        val guids = OrganizationDao.findAll(Authorization.All, limit = 1000).map(_.guid)
        guids.contains(publicOrg.guid) should be(true)
        guids.contains(privateOrg.guid) should be(true)
      }

    }

    describe("PublicOnly") {

      it("sees only the public org") {
        val guids = OrganizationDao.findAll(Authorization.PublicOnly, limit = 1000).map(_.guid)
        guids.contains(publicOrg.guid) should be(true)
        guids.contains(privateOrg.guid) should be(false)
      }

    }

    describe("User") {

      it("user can see own org") {
        val guids = OrganizationDao.findAll(Authorization.User(privateUser.guid)).map(_.guid)
        guids.contains(publicOrg.guid) should be(true)
        guids.contains(privateOrg.guid) should be(true)
      }

      it("other user cannot see private org") {
        val guids = OrganizationDao.findAll(Authorization.User(publicUser.guid)).map(_.guid)
        guids.contains(publicOrg.guid) should be(true)
        guids.contains(privateOrg.guid) should be(false)
      }

    }

  }

}
