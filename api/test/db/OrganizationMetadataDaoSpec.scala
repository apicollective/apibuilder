package db

import com.gilt.apidoc.models.{Organization, OrganizationMetadata, OrganizationMetadataForm, Visibility}
import lib.Validation
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID
import org.postgresql.util.PSQLException

class OrganizationMetadataDaoSpec extends FunSpec with Matchers {

  def upsertOrganizationMetadata(org: Organization): OrganizationMetadata = {
    val form = OrganizationMetadataForm(
      visibility = None,
      packageName = None
    )
    OrganizationMetadataDao.upsert(Util.createdBy, org, form)
  }

  describe("create") {

    val form = OrganizationMetadataForm(
      visibility = None,
      packageName = None
    )

    describe("packageName") {

      it("defaults to None") {
        val org = Util.createOrganization()
        OrganizationMetadataDao.create(Util.createdBy, org, form)
        OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(None)
      }

      it("can set") {
        val org = Util.createOrganization()
        OrganizationMetadataDao.create(Util.createdBy, org, form.copy(packageName = Some("com.gilt")))
        OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.gilt"))
      }
    }

    describe("visibility") {

      it ("defaults to none") {
        val org = Util.createOrganization()
        OrganizationMetadataDao.create(Util.createdBy, org, form)
        upsertOrganizationMetadata(org).visibility should be(None)
      }

      it("can set") {
        val org = Util.createOrganization()

        OrganizationMetadataDao.create(Util.createdBy, org, form.copy(visibility = Some(Visibility.Public)))
        OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.visibility should be(Some(Visibility.Public))

        OrganizationMetadataDao.upsert(Util.createdBy, org, form.copy(visibility = Some(Visibility.Organization)))
        OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.visibility should be(Some(Visibility.Organization))
      }

    }

  }

  it("upsert") {
    val org = Util.createOrganization()
    val form = OrganizationMetadataForm(
      packageName = Some("com.giltgroupe")
    )

    OrganizationMetadataDao.upsert(Util.createdBy, org, form)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.giltgroupe"))

    OrganizationMetadataDao.upsert(Util.createdBy, org, form.copy(packageName = Some("com.gilt")))
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).get.packageName should be(Some("com.gilt"))
  }

  it("raises error on duplicate") {
    val org = Util.createOrganization()
    upsertOrganizationMetadata(org)

    intercept[PSQLException] {
      OrganizationMetadataDao.create(Util.createdBy, org, OrganizationMetadataForm())
    }

    // Now verify we can re-create after deleting
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    OrganizationMetadataDao.create(Util.createdBy, org, OrganizationMetadataForm())
  }

  it("softDelete") {
    val org = Util.createOrganization()
    upsertOrganizationMetadata(org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).isEmpty should be(false)
    OrganizationMetadataDao.softDelete(Util.createdBy, org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).isEmpty should be(true)
  }

  it("findByOrganizationGuid") {
    val org = Util.createOrganization()
    OrganizationMetadataDao.findByOrganizationGuid(org.guid) should be(None)
    upsertOrganizationMetadata(org)
    OrganizationMetadataDao.findByOrganizationGuid(org.guid).isEmpty should be(false)
  }

  describe("OrganizationMetadataForm") {
    val form = OrganizationMetadataForm(
      visibility = None,
      packageName = None
    )

    describe("packageName") {

      it("no package name") {
        OrganizationMetadataDao.validate(form) should be(Seq.empty)
      }

      it("valid package name") {
        OrganizationMetadataDao.validate(form.copy(packageName = Some("com.gilt"))) should be(Seq.empty)
        OrganizationMetadataDao.validate(form.copy(packageName = Some("com.gilt.foo"))) should be(Seq.empty)
        OrganizationMetadataDao.validate(form.copy(packageName = Some("com.gilt.foo.bar"))) should be(Seq.empty)
        OrganizationMetadataDao.validate(form.copy(packageName = Some("me.apidoc"))) should be(Seq.empty)
      }

      it("invalid package name") {
        OrganizationMetadataDao.validate(form.copy(packageName = Some("com gilt"))) should be("Domain com gilt is not valid. Expected a domain name like apidoc.me")
      }

    }

  }

}
