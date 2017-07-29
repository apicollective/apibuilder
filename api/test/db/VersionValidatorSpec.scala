package db

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class VersionValidatorSpec extends FunSpec with Matchers with util.TestApplication {

  it("validates user is a member of the organization to create an application") {
    val org = Util.createOrganization()

    VersionValidator(Util.createRandomUser(), org, UUID.randomUUID.toString).validate should be(
      Seq("You must be a member of this organization to update applications")
    )
  }

  describe("key") {

    it("validates if already exists") {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, existing.key).validate should be(
        Seq(s"An application with key[${existing.key}] already exists")
      )
    }

    it("no errors for a new key") {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, UUID.randomUUID.toString).validate should be(Nil)
    }

    it("no errors when updating an application with the same key") {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, existing.key, Some(existing.key)).validate should be(Nil)
    }

    it("validates that key is not changing") {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      val newKey = UUID.randomUUID.toString
      VersionValidator(Util.createdBy, org, newKey, Some(existing.key)).validate should be(
        Seq(s"The application key[$newKey] in the uploaded file does not match the existing application key[${existing.key}]. If you would like to change the key of an application, delete the existing application and then create a new one")
      )
    }

  }

}
