package db

import helpers.OrganizationHelpers

import java.util.UUID
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class OrganizationDomainsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with OrganizationHelpers {

  "create" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = createOrganization()
    val domain = organizationDomainsDao.create(testUser, org, domainName)
    domain.domain.name must be(domainName)

    organizationDomainsDao.findAll(guid = Some(domain.guid)).map(_.guid) must be(Seq(domain.guid))

    organizationDomainsDao.softDelete(testUser, domain)
    organizationDomainsDao.findAll(guid = Some(domain.guid)) must be(Nil)
  }

  "domains are unique per org" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org1 = createOrganization()
    organizationDomainsDao.create(testUser, org1, domainName)

    val org2 = createOrganization()
    organizationDomainsDao.create(testUser, org2, domainName)

    def getDomains(orgGuid: UUID) = toModel(
        organizationsDao.findByGuid(Authorization.All, orgGuid).get
      ).domains.map(_.name)

    getDomains(org1.guid) must be(Seq(domainName))
    getDomains(org2.guid) must be(Seq(domainName))
  }

  "findAll" in {
    val domainName = UUID.randomUUID.toString + ".org"
    val org = createOrganization()
    val domain = organizationDomainsDao.create(testUser, org, domainName)

    organizationDomainsDao.findAll(organizationGuid = Some(org.guid)).map(_.guid) must be(Seq(domain.guid))
    organizationDomainsDao.findAll(organizationGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
  }

}
