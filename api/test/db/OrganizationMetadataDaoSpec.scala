package db

import com.gilt.apidoc.models.OrganizationMetadata
import lib.Validation
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
    createOrganizationMetadata(org).packageName should be(Some("com.gilt"))
  }

  it("upsert") {
    val org = Util.createOrganization()
    val form = OrganizationMetadataForm(
      package_name = Some("com.giltgroupe")
    )

    OrganizationMetadataDao.upsert(Util.createdBy, org, form)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.giltgroupe"))

    OrganizationMetadataDao.upsert(Util.createdBy, org, form.copy(package_name = Some("com.gilt")))
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.gilt"))
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
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.gilt"))
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
  }

  it("findByOrganizationGuid") {
    val org = Util.createOrganization()
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
    createOrganizationMetadata(org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.gilt"))
  }

  describe("OrganizationMetadataForm") {
    val form = OrganizationMetadataForm(
      package_name = None
    )

    it("no package name") {
      OrganizationMetadataForm.validate(form) should be(Seq.empty)
    }

    it("valid package name") {
      OrganizationMetadataForm.validate(form.copy(package_name = Some("com.gilt"))) should be(Seq.empty)
      OrganizationMetadataForm.validate(form.copy(package_name = Some("com.gilt.foo"))) should be(Seq.empty)
      OrganizationMetadataForm.validate(form.copy(package_name = Some("com.gilt.foo.bar"))) should be(Seq.empty)
      OrganizationMetadataForm.validate(form.copy(package_name = Some("me.apidoc"))) should be(Seq.empty)
    }

    it("invalid package name") {
      OrganizationMetadataForm.validate(form.copy(package_name = Some("com gilt"))) should be(Validation.invalidName())
    }
  }

}
