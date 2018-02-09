package db

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.junit.Assert._
import java.util.UUID

class OrganizationDomainsDaoSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  "create" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = createOrganization()
    val domain = organizationDomainsDao.create(createdBy, org, domainName)
    domain.domain.name must be(domainName)

    organizationDomainsDao.findAll(guid = Some(domain.guid)).map(_.guid) must be(Seq(domain.guid))

    organizationDomainsDao.softDelete(createdBy, domain)
    organizationDomainsDao.findAll(guid = Some(domain.guid)) must be(Nil)
  }

  "domains are unique per org" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org1 = createOrganization()
    val domain1 = organizationDomainsDao.create(createdBy, org1, domainName)

    val org2 = createOrganization()
    val domain2 = organizationDomainsDao.create(createdBy, org2, domainName)

    organizationsDao.findByGuid(Authorization.All, org1.guid).get.domains.map(_.name) must be(Seq(domainName))
    organizationsDao.findByGuid(Authorization.All, org2.guid).get.domains.map(_.name) must be(Seq(domainName))
  }

  "findAll" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = createOrganization()
    val domain = organizationDomainsDao.create(createdBy, org, domainName)

    organizationDomainsDao.findAll(organizationGuid = Some(org.guid)).map(_.guid) must be(Seq(domain.guid))
    organizationDomainsDao.findAll(organizationGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
  }

}
