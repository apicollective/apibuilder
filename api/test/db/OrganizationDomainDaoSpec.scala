package db

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class OrganizationDomainsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("create") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = OrganizationDomainsDao.create(Util.createdBy, org, domainName)
    domain.domain should be(domainName)

    OrganizationDomainsDao.findAll(guid = Some(domain.guid)).map(_.guid) should be(Seq(domain.guid))

    OrganizationDomainsDao.softDelete(Util.createdBy, domain)
    OrganizationDomainsDao.findAll(guid = Some(domain.guid)) should be(Seq.empty)
  }

  it("findAll") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = OrganizationDomainsDao.create(Util.createdBy, org, domainName)

    OrganizationDomainsDao.findAll(organizationGuid = Some(org.guid)).map(_.guid) should be(Seq(domain.guid))
    OrganizationDomainsDao.findAll(organizationGuid = Some(UUID.randomUUID)).map(_.guid) should be(Seq.empty)
  }

}
