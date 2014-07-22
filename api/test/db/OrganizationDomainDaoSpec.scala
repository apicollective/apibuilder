package db

import org.scalatest.{ FunSpec, Matchers }
import org.junit.Assert._
import java.util.UUID

class OrganizationDomainDaoSpec extends FunSpec with Matchers {

  it("create") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = OrganizationDomainDao.create(Util.createdBy, org, domainName)
    domain.domain should be(domainName)

    OrganizationDomainDao.findAll(guid = Some(domain.guid)).map(_.guid) should be(Seq(domain.guid))

    OrganizationDomainDao.softDelete(Util.createdBy, domain)
    OrganizationDomainDao.findAll(guid = Some(domain.guid)) should be(Seq.empty)
  }

  it("findAll") {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = Util.createOrganization()
    val domain = OrganizationDomainDao.create(Util.createdBy, org, domainName)

    OrganizationDomainDao.findAll(organizationGuid = Some(org.guid)).map(_.guid) should be(Seq(domain.guid))
    OrganizationDomainDao.findAll(organizationGuid = Some(UUID.randomUUID.toString)).map(_.guid) should be(Seq.empty)
  }

}
