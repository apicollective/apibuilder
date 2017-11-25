package db

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class OrganizationDomainsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  it("create") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = organizationDomainsDao.create(Util.createdBy, org, domainName)
    domain.domain.name should be(domainName)

    organizationDomainsDao.findAll(guid = Some(domain.guid)).map(_.guid) should be(Seq(domain.guid))

    organizationDomainsDao.softDelete(Util.createdBy, domain)
    organizationDomainsDao.findAll(guid = Some(domain.guid)) should be(Nil)
  }

  it("domains are unique per org") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org1 = Util.createOrganization()
    val domain1 = organizationDomainsDao.create(Util.createdBy, org1, domainName)

    val org2 = Util.createOrganization()
    val domain2 = organizationDomainsDao.create(Util.createdBy, org2, domainName)

    organizationsDao.findByGuid(Authorization.All, org1.guid).get.domains.map(_.name) should be(Seq(domainName))
    organizationsDao.findByGuid(Authorization.All, org2.guid).get.domains.map(_.name) should be(Seq(domainName))
  }

  it("findAll") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = organizationDomainsDao.create(Util.createdBy, org, domainName)

    organizationDomainsDao.findAll(organizationGuid = Some(org.guid)).map(_.guid) should be(Seq(domain.guid))
    organizationDomainsDao.findAll(organizationGuid = Some(UUID.randomUUID)).map(_.guid) should be(Nil)
  }

}
