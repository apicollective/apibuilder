package db

import io.apibuilder.api.v0.models.{OrganizationForm, Visibility}
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class OrganizationsDaoSpec extends FunSpec with Matchers with util.TestApplication {

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

  describe("update") {

    lazy val org = Util.createOrganization()
    lazy val form = OrganizationForm(
      name = org.name,
      key = Some(org.key),
      visibility = org.visibility,
      namespace = org.namespace
    )

    it("name") {
      val updated = organizationsDao.update(Util.createdBy, org, form.copy(name = org.name + "2"))
      updated.name should be(org.name + "2")
    }

    it("key") {
      val updated = organizationsDao.update(Util.createdBy, org, form.copy(key = Some(org.key + "2")))
      updated.key should be(org.key + "2")
    }

    it("namespace") {
      val updated = organizationsDao.update(Util.createdBy, org, form.copy(namespace = org.namespace + "2"))
      updated.namespace should be(org.namespace + "2")
    }
  
    it("visibility") {
      val updated = organizationsDao.update(Util.createdBy, org, form.copy(visibility = Visibility.Public))
      updated.visibility should be(Visibility.Public)

      val updated2 = organizationsDao.update(Util.createdBy, org, form.copy(visibility = Visibility.Organization))
      updated2.visibility should be(Visibility.Organization)
    }

  }

  it("user that creates org should be an admin") {
    val user = Util.upsertUser(UUID.randomUUID.toString + "@test.apidoc.me")
    val name = UUID.randomUUID.toString
    val org = organizationsDao.createWithAdministrator(user, Util.createOrganizationForm(name = name))
    org.name should be(name)

    membershipsDao.isUserAdmin(user, org) should be(true)
  }

  describe("domain") {

    val domainName = UUID.randomUUID.toString
    val domains = Seq(domainName + ".com", UUID.randomUUID.toString + ".org")
    val org = organizationsDao.createWithAdministrator(
      Util.createdBy,
      OrganizationForm(
        name = "Test Org " + UUID.randomUUID.toString,
        domains = Some(domains),
        namespace = "test." +UUID.randomUUID.toString
      )
    )

    it("creates with domains") {
      org.domains.map(_.name).mkString(" ") should be(domains.mkString(" "))
      val fetched = organizationsDao.findByGuid(Authorization.All, org.guid).get
      fetched.domains.map(_.name).sorted.mkString(" ") should be(domains.sorted.mkString(" "))
    }

    it("defaults visibility to organization") {
      val fetched = organizationsDao.findByGuid(Authorization.All, org.guid).get
      fetched.visibility should be(Visibility.Organization)
    }

  }

  it("find by guid") {
    assertEquals(organizationsDao.findByGuid(Authorization.All, Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  describe("findAll") {

    val user1 = Util.createRandomUser()
    val org1 = Util.createOrganization(user1)

    val user2 = Util.createRandomUser()
    val org2 = Util.createOrganization(user2)

    it("by key") {
      organizationsDao.findAll(Authorization.All, key = Some(org1.key)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, key = Some(org2.key)).map(_.guid) should be(Seq(org2.guid))
      organizationsDao.findAll(Authorization.All, key = Some(UUID.randomUUID.toString)) should be(Seq.empty)
    }

    it("by name") {
      organizationsDao.findAll(Authorization.All, name = Some(org1.name)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, name = Some(org1.name.toUpperCase)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, name = Some(org1.name.toLowerCase)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, name = Some(org2.name)).map(_.guid) should be(Seq(org2.guid))
      organizationsDao.findAll(Authorization.All, name = Some(UUID.randomUUID.toString)) should be(Seq.empty)
    }

    it("by namespace") {
      organizationsDao.findAll(Authorization.All, namespace = Some(org1.namespace)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, namespace = Some(org1.namespace.toUpperCase)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, namespace = Some(org1.namespace.toLowerCase)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, namespace = Some(org2.namespace)).map(_.guid) should be(Seq(org2.guid))
      organizationsDao.findAll(Authorization.All, namespace = Some(UUID.randomUUID.toString)) should be(Seq.empty)
    }

    it("by guid") {
      organizationsDao.findAll(Authorization.All, guid = Some(org1.guid)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, guid = Some(org2.guid)).map(_.guid) should be(Seq(org2.guid))
      organizationsDao.findAll(Authorization.All, guid = Some(UUID.randomUUID)) should be(Seq.empty)
    }

    it("by userGuid") {
      organizationsDao.findAll(Authorization.All, userGuid = Some(user1.guid)).map(_.guid) should be(Seq(org1.guid))
      organizationsDao.findAll(Authorization.All, userGuid = Some(user2.guid)).map(_.guid) should be(Seq(org2.guid))
      organizationsDao.findAll(Authorization.All, userGuid = Some(UUID.randomUUID)) should be(Seq.empty)
    }
  }

  describe("validation") {

    it("validates name") {
      organizationsDao.validate(Util.createOrganizationForm(name = "this is a long name")) should be(Seq.empty)
      organizationsDao.validate(Util.createOrganizationForm(name = "a")).head.message should be("name must be at least 3 characters")
      organizationsDao.validate(Util.createOrganizationForm(name = Util.gilt.name)).head.message should be("Org with this name already exists")

      organizationsDao.validate(Util.createOrganizationForm(name = Util.gilt.name), Some(Util.gilt)) should be(Seq.empty)
    }

    it("validates key") {
      organizationsDao.validate(Util.createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a"))).head.message should be("Key must be at least 3 characters")
      organizationsDao.validate(Util.createOrganizationForm(name = UUID.randomUUID.toString, key = Some(Util.gilt.key))).head.message should be("Org with this key already exists")
      organizationsDao.validate(Util.createOrganizationForm(name = UUID.randomUUID.toString, key = Some(Util.gilt.key)), Some(Util.gilt)) should be(Seq.empty)
    }

    it("raises error if you try to create an org with a short name") {
      intercept[java.lang.AssertionError] {
        organizationsDao.createWithAdministrator(Util.createdBy, Util.createOrganizationForm("a"))
      }.getMessage should be("assertion failed: name must be at least 3 characters")
    }

    it("isDomainValid") {
      organizationsDao.isDomainValid("bryzek.com") should be(true)
      organizationsDao.isDomainValid("gilt.org") should be(true)
      organizationsDao.isDomainValid("www.bryzek.com") should be(true)
      organizationsDao.isDomainValid("WWW.GILT.COM") should be(true)
      organizationsDao.isDomainValid("www gilt com") should be(false)
    }

    it("validates domains") {
      val name = UUID.randomUUID.toString
      organizationsDao.validate(Util.createOrganizationForm(name = name, domains = None)) should be(Seq.empty)
      organizationsDao.validate(Util.createOrganizationForm(name = name, domains = Some(Seq("bad name")))).head.message should be("Domain bad name is not valid. Expected a domain name like apidoc.me")
    }
  }

  describe("Authorization") {

    val publicUser = Util.createRandomUser()
    val publicOrg = Util.createOrganization(publicUser, Some("A Public " + UUID.randomUUID().toString), visibility = Visibility.Public)

    val privateUser = Util.createRandomUser()
    val privateOrg = Util.createOrganization(privateUser, Some("A Private " + UUID.randomUUID().toString))

    describe("All") {

      it("sees both orgs") {
        organizationsDao.findAll(Authorization.All, guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        organizationsDao.findAll(Authorization.All, guid = Some(privateOrg.guid)).map(_.guid) should be(Seq(privateOrg.guid))
      }

    }

    describe("PublicOnly") {

      it("sees only the public org") {
        organizationsDao.findAll(Authorization.PublicOnly, guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        organizationsDao.findAll(Authorization.PublicOnly, guid = Some(privateOrg.guid)).map(_.guid) should be(Seq.empty)
      }

    }

    describe("User") {

      it("user can see own org") {
        organizationsDao.findAll(Authorization.User(privateUser.guid), guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        organizationsDao.findAll(Authorization.User(privateUser.guid), guid = Some(privateOrg.guid)).map(_.guid) should be(Seq(privateOrg.guid))
      }

      it("other user cannot see private org") {
        organizationsDao.findAll(Authorization.User(publicUser.guid), guid = Some(publicOrg.guid)).map(_.guid) should be(Seq(publicOrg.guid))
        organizationsDao.findAll(Authorization.User(publicUser.guid), guid = Some(privateOrg.guid)).map(_.guid) should be(Seq.empty)
      }

    }

  }

}
