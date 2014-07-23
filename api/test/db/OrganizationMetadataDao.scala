package db

import org.scalatest.{ FunSpec, Matchers }
import org.junit.Assert._
import java.util.UUID
import org.postgresql.util.PSQLException

class OrganizationMetadataDaoSpec extends FunSpec with Matchers {

  it("create") {
    val org = Util.createOrganization()
    val metadata = OrganizationMetadataDao.create(Util.createdBy, OrganizationMetadataForm(
      organization_guid = org.guid,
      package_name = Some("com.gilt")
    ))
    metadata.package_name should be(Some("com.gilt"))
  }

  it("raises error on duplicate") {
    val org = Util.createOrganization()
    val form = OrganizationMetadataForm(
      organization_guid = org.guid,
      package_name = Some("com.gilt")
    )
    OrganizationMetadataDao.create(Util.createdBy, form)

    intercept[PSQLException] {
      OrganizationMetadataDao.create(Util.createdBy, form)
    }

    // Now verify we can re-create after deleting
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    OrganizationMetadataDao.create(Util.createdBy, form)
  }

  it("softDelete") {
    val org = Util.createOrganization()
    val form = OrganizationMetadataForm(
      organization_guid = org.guid,
      package_name = Some("com.gilt")
    )
    OrganizationMetadataDao.create(Util.createdBy, form)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.package_name should be(Some("com.gilt"))

    OrganizationMetadataDao.softDelete(Util.createdBy, org)

    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
  }

  it("findByOrganizationGuid") {
    val org = Util.createOrganization()
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)

    OrganizationMetadataDao.create(Util.createdBy, OrganizationMetadataForm(
      organization_guid = org.guid,
      package_name = Some("com.gilt")
    ))

    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.package_name should be(Some("com.gilt"))
  }

}
