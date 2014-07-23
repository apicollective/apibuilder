package db

import org.scalatest.{ FunSpec, Matchers }
import org.junit.Assert._
import java.util.UUID
import org.postgresql.util.PSQLException

class OrganizationMetadataDaoSpec extends FunSpec with Matchers {

  def createOrganizationMetadata(org: Organization): OrganizationMetadata = {
    val form = OrganizationMetadataForm(
      package_name = Some("com.gilt")
    )
    OrganizationMetadataDao.create(Util.createdBy, org, form)
  }

  it("create") {
    val org = Util.createOrganization()
    createOrganizationMetadata(org).package_name should be(Some("com.gilt"))
  }

  it("raises error on duplicate") {
    val org = Util.createOrganization()
    createOrganizationMetadata(org)

    intercept[PSQLException] {
      createOrganizationMetadata(org)
    }

    // Now verify we can re-create after deleting
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    createOrganizationMetadata(org)
  }

  it("softDelete") {
    val org = Util.createOrganization()
    createOrganizationMetadata(org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.package_name should be(Some("com.gilt"))
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
  }

  it("findByOrganizationGuid") {
    val org = Util.createOrganization()
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
    createOrganizationMetadata(org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.package_name should be(Some("com.gilt"))
  }

}
