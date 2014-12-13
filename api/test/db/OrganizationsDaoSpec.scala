package db

import com.gilt.apidoc.models.{OrganizationForm, OrganizationMetadataForm, Visibility}
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class OrganizationsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("create") {
    Util.gilt.name should be("Gilt Test Org")
    Util.gilt.key should be("gilt-test-org")
  }

  it("create w/ explicit key") {
    val name = UUID.randomUUID.toString
    val key = "key-" + UUID.randomUUID.toString
    val org = Util.createOrganization(Util.createdBy, name = Some(name), key = Some(key))
    org.name should be(name)
    org.key should be(key)
  }

  it("user that creates org should be an admin") {
    val user = Util.upsertUser(UUID.randomUUID.toString + "@gilttest.com")
    val name = UUID.randomUUID.toString
    val org = OrganizationsDao.createWithAdministrator(user, OrganizationForm(name = name))
    org.name should be(name)

    MembershipsDao.isUserAdmin(user, org) should be(true)
  }

  it("reverseDomain") {
    OrganizationsDao.reverseDomain("gilt.com") should be("com.gilt")
    OrganizationsDao.reverseDomain("foo.gilt.com") should be("com.gilt.foo")

  }

  describe("domain") {

    val domainName = UUID.randomUUID.toString
    val domains = Seq(domainName + ".com", UUID.randomUUID.toString + ".org")
    val org = OrganizationsDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        domains = domains
      )
    )

    it("creates with domains") {
      org.domains.map(_.name).mkString(" ") should be(domains.mkString(" "))
      val fetched = OrganizationsDao.findByGuid(Authorization.All, org.guid).get
      fetched.domains.map(_.name).sorted.mkString(" ") should be(domains.sorted.mkString(" "))
    }

    it("defaults metadata.package_name to reverse of first domain if provided") {
      val fetched = OrganizationsDao.findByGuid(Authorization.All, org.guid).get
      fetched.metadata.get.packageName should be(Some("com." + domainName))
    }

  }

  it("creates with metadata") {
    val org = OrganizationsDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        metadata = Some(
          OrganizationMetadataForm(
            packageName = Some("com.gilt")
          )
        )
      )
    )

    org.metadata.get.packageName should be(Some("com.gilt"))
    val fetched = OrganizationsDao.findByGuid(Authorization.All, org.guid).get
    fetched.metadata.get.packageName should be(Some("com.gilt"))
  }

  it("find by guid") {
    assertEquals(OrganizationsDao.findByGuid(Authorization.All, Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  describe("findAll") {

    val user1 = Util.createRandomUser()
    val org1 = Util.createOrganization(user1)

    val user2 = Util.createRandomUser()
    val org2 = Util.createOrganization(user2)

    it("by key") {
      OrganizationsDao.findAll(Authorization.All, key = Some(org1.key)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, key = Some(org2.key)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by name") {
      OrganizationsDao.findAll(Authorization.All, name = Some(org1.name)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, name = Some(org1.name.toUpperCase)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, name = Some(org1.name.toLowerCase)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, name = Some(org2.name)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by guid") {
      OrganizationsDao.findAll(Authorization.All, guid = Some(org1.guid)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, guid = Some(org2.guid)).map(_.guid) should be(Seq(org2.guid))
    }

    it("by userGuid") {
      OrganizationsDao.findAll(Authorization.All, userGuid = Some(user1.guid)).map(_.guid) should be(Seq(org1.guid))
      OrganizationsDao.findAll(Authorization.All, userGuid = Some(user2.guid)).map(_.guid) should be(Seq(org2.guid))
    }
  }

  it("emailDomain") {
    OrganizationsDao.emailDomain("mb@gilt.com") should be(Some("gilt.com"))
    OrganizationsDao.emailDomain("mb@internal.gilt.com") should be(Some("internal.gilt.com"))
    OrganizationsDao.emailDomain("mb") should be(None)
  }

  describe("validation") {

    it("validates name") {
      OrganizationsDao.validate(OrganizationForm(name = "this is a long name")) should be(Seq.empty)
      OrganizationsDao.validate(OrganizationForm(name = "a")).head.message should be("name must be at least 4 characters")
      OrganizationsDao.validate(OrganizationForm(name = Util.gilt.name)).head.message should be("Org with this name already exists")
    }

    it("raises error if you try to create an org with a short name") {
      intercept[java.lang.IllegalArgumentException] {
        OrganizationsDao.createWithAdministrator(Util.createdBy, OrganizationForm("a"))
      }.getMessage should be("requirement failed: Name too short")
    }

    it("isDomainValid") {
      OrganizationsDao.isDomainValid("gilt.com") should be(true)
      OrganizationsDao.isDomainValid("gilt.org") should be(true)
      OrganizationsDao.isDomainValid("www.gilt.com") should be(true)
      OrganizationsDao.isDomainValid("WWW.GILT.COM") should be(true)
      OrganizationsDao.isDomainValid("www gilt com") should be(false)
    }

    it("validates domains") {
      val name = UUID.randomUUID.toString
      OrganizationsDao.validate(OrganizationForm(name = name, domains = Seq.empty)) should be(Seq.empty)
      OrganizationsDao.validate(OrganizationForm(name = name, domains = Seq.empty)) should be(Seq.empty)
      OrganizationsDao.validate(OrganizationForm(name = name, domains = Seq("bad name"))).head.message should be("Domain bad name is not valid. Expected a domain name like apidoc.me")
    }
  }

  describe("Authorization") {

    val publicUser = Util.createRandomUser()
    val publicOrg = Util.createOrganization(publicUser, Some("Public " + UUID.randomUUID().toString))
    OrganizationMetadataDao.create(Util.createdBy, publicOrg, OrganizationMetadataForm(visibility = Some(Visibility.Public)))

    val privateUser = Util.createRandomUser()
    val privateOrg = Util.createOrganization(privateUser, Some("Private " + UUID.randomUUID().toString))

    describe("All") {

      it("sees both orgs") {
        OrganizationsDao.findAll(Authorization.All, guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        OrganizationsDao.findAll(Authorization.All, guid = Some(privateOrg.guid)).map(_.guid) should be(Seq(privateOrg.guid))
      }

    }

    describe("PublicOnly") {

      it("sees only the public org") {
        OrganizationsDao.findAll(Authorization.PublicOnly, guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        OrganizationsDao.findAll(Authorization.PublicOnly, guid = Some(privateOrg.guid)).map(_.guid) should be(Seq.empty)
      }

    }

    describe("User") {

      it("user can see own org") {
        OrganizationsDao.findAll(Authorization.User(privateUser.guid), guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        OrganizationsDao.findAll(Authorization.User(privateUser.guid), guid = Some(privateOrg.guid)).map(_.guid) should be(Seq(privateOrg.guid))
      }

      it("other user cannot see private org") {
        OrganizationsDao.findAll(Authorization.User(publicUser.guid), guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        OrganizationsDao.findAll(Authorization.User(publicUser.guid), guid = Some(privateOrg.guid)).map(_.guid) should be(Seq.empty)
      }

    }

  }

}
